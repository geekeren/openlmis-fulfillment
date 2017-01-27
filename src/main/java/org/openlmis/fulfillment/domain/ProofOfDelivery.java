package org.openlmis.fulfillment.domain;


import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.openlmis.fulfillment.domain.convert.LocalDatePersistenceConverter;
import org.openlmis.fulfillment.web.util.OrderDto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;


@Entity
@NoArgsConstructor
@Table(name = "proof_of_deliveries")
public class ProofOfDelivery extends BaseEntity {
  public static final String DELIVERED_BY = "deliveredBy";
  public static final String RECEIVED_BY = "receivedBy";
  public static final String RECEIVED_DATE = "receivedDate";
  public static final String PROOF_OF_DELIVERY_LINE_ITEMS = "proofOfDeliveryLineItems";

  @OneToOne
  @JoinColumn(name = "orderId", nullable = false)
  @Getter
  @Setter
  private Order order;

  @OneToMany(
      mappedBy = "proofOfDelivery",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.EAGER,
      orphanRemoval = true)
  @Fetch(FetchMode.SELECT)
  @Getter
  @Setter
  private List<ProofOfDeliveryLineItem> proofOfDeliveryLineItems;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String deliveredBy;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  @Getter
  @Setter
  private String receivedBy;

  @Getter
  @Setter
  @Convert(converter = LocalDatePersistenceConverter.class)
  private LocalDate receivedDate;

  /**
   * Creates a new instance of Proof Of Delivery based on the passed order.
   *
   * @param order instance that would be used to create new Proof Of Delivery.
   */
  public ProofOfDelivery(Order order) {
    this.order = order;
    this.proofOfDeliveryLineItems = order
        .getOrderLineItems()
        .stream()
        .map(line -> new ProofOfDeliveryLineItem(this, line))
        .collect(Collectors.toList());
  }

  @PrePersist
  private void prePersist() {
    forEachLine(line -> line.setProofOfDelivery(this));
  }

  @PreUpdate
  private void preUpdate() {
    forEachLine(line -> line.setProofOfDelivery(this));
  }

  /**
   * Copy values of attributes into new or updated ProofOfDelivery.
   *
   * @param proofOfDelivery ProofOfDelivery with new values.
   */
  public void updateFrom(ProofOfDelivery proofOfDelivery) {
    this.deliveredBy = proofOfDelivery.getDeliveredBy();
    this.receivedBy = proofOfDelivery.getReceivedBy();
    this.receivedDate = proofOfDelivery.getReceivedDate();

    updateLines(proofOfDelivery.getProofOfDeliveryLineItems());

  }

  public void forEachLine(Consumer<ProofOfDeliveryLineItem> consumer) {
    Optional.ofNullable(proofOfDeliveryLineItems)
        .ifPresent(list -> list.forEach(consumer));
  }

  private void updateLines(Collection<ProofOfDeliveryLineItem> newLineItems) {
    if (null == newLineItems) {
      return;
    }

    if (null == proofOfDeliveryLineItems) {
      proofOfDeliveryLineItems = new ArrayList<>();
    }

    for (ProofOfDeliveryLineItem item : newLineItems) {
      ProofOfDeliveryLineItem existing = proofOfDeliveryLineItems
          .stream()
          .filter(l -> l.getId().equals(item.getId()))
          .findFirst().orElse(null);

      if (null != existing) {
        existing.setProofOfDelivery(this);
        existing.updateFrom(item);
      }
    }
  }

  /**
   * Create instance of ProofOfDelivery based on given {@link ProofOfDelivery.Importer}
   *
   * @param importer instance of {@link ProofOfDelivery.Importer}
   * @return instance of ProofOfDelivery.
   */
  public static ProofOfDelivery newInstance(Importer importer) {
    ProofOfDelivery proofOfDelivery = new ProofOfDelivery();
    proofOfDelivery.setOrder(Order.newInstance(importer.getOrder()));
    proofOfDelivery.setDeliveredBy(importer.getDeliveredBy());
    proofOfDelivery.setReceivedBy(importer.getReceivedBy());
    proofOfDelivery.setReceivedDate(importer.getReceivedDate());

    if (importer.getProofOfDeliveryLineItems() != null) {
      proofOfDelivery.setProofOfDeliveryLineItems(new ArrayList<>());
      importer.getProofOfDeliveryLineItems().forEach(
          podli -> proofOfDelivery.getProofOfDeliveryLineItems().add(
              ProofOfDeliveryLineItem.newInstance(podli)));
    }

    return proofOfDelivery;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setDeliveredBy(deliveredBy);
    exporter.setReceivedBy(receivedBy);
    exporter.setReceivedDate(receivedDate);
  }

  public interface Exporter {
    void setId(UUID id);

    void setDeliveredBy(String deliveredBy);

    void setReceivedBy(String receivedBy);

    void setReceivedDate(LocalDate receivedDate);

  }

  public interface Importer {
    UUID getId();

    OrderDto getOrder();

    List<ProofOfDeliveryLineItem.Importer> getProofOfDeliveryLineItems();

    String getDeliveredBy();

    String getReceivedBy();

    LocalDate getReceivedDate();

  }
}
