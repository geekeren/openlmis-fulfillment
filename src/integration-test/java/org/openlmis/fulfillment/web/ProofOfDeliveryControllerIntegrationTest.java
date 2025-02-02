/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.fulfillment.web;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.openlmis.fulfillment.i18n.MessageKeys.MUST_CONTAIN_VALUE;
import static org.openlmis.fulfillment.i18n.MessageKeys.PERMISSIONS_MISSING;
import static org.openlmis.fulfillment.i18n.MessageKeys.PERMISSION_MISSING;
import static org.openlmis.fulfillment.i18n.MessageKeys.PROOF_OF_DELIVERY_ALREADY_CONFIRMED;
import static org.openlmis.fulfillment.service.PermissionService.PODS_MANAGE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.google.common.collect.ImmutableSet;
import guru.nidi.ramltester.junit.RamlMatchers;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import net.sf.jasperreports.engine.JRParameter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openlmis.fulfillment.ProofOfDeliveryDataBuilder;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ProofOfDeliveryStatus;
import org.openlmis.fulfillment.domain.Template;
import org.openlmis.fulfillment.domain.TemplateParameter;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.repository.ProofOfDeliveryRepository;
import org.openlmis.fulfillment.repository.ShipmentRepository;
import org.openlmis.fulfillment.service.FulfillmentNotificationService;
import org.openlmis.fulfillment.service.JasperReportsViewService;
import org.openlmis.fulfillment.service.PageDto;
import org.openlmis.fulfillment.service.PermissionService;
import org.openlmis.fulfillment.service.ProofOfDeliveryService;
import org.openlmis.fulfillment.service.referencedata.PermissionStringDto;
import org.openlmis.fulfillment.service.referencedata.PermissionStrings;
import org.openlmis.fulfillment.service.stockmanagement.StockEventStockManagementService;
import org.openlmis.fulfillment.util.Pagination;
import org.openlmis.fulfillment.web.stockmanagement.StockEventDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.openlmis.fulfillment.web.util.StockEventBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.view.jasperreports.JasperReportsMultiFormatView;

@SuppressWarnings("PMD.TooManyMethods")
public class ProofOfDeliveryControllerIntegrationTest extends BaseWebIntegrationTest {
  private static final String RESOURCE_URL = "/api/proofsOfDelivery";
  private static final String ID_URL = RESOURCE_URL + "/{id}";
  private static final String PRINT_URL = ID_URL + "/print";
  private static final String AUDIT_LOG_URL = ID_URL + "/auditLog";

  private static final String MESSAGE_KEY = "messageKey";
  private static final String PARAM_PAGE = "page";
  private static final String PARAM_SIZE = "size";

  @MockBean
  private ProofOfDeliveryRepository proofOfDeliveryRepository;

  @MockBean
  private ProofOfDeliveryService proofOfDeliveryService;

  @MockBean
  private ShipmentRepository shipmentRepository;

  @MockBean
  private OrderRepository orderRepository;

  @MockBean
  private StockEventBuilder stockEventBuilder;

  @MockBean
  private StockEventStockManagementService stockEventStockManagementService;

  @SpyBean
  private PermissionService permissionService;

  @MockBean
  private PermissionStrings.Handler permissionStringsHandler;

  @MockBean
  private FulfillmentNotificationService fulfillmentNotificationService;

  @MockBean
  private JasperReportsViewService jasperReportsViewService;

  @Value("${service.url}")
  private String serviceUrl;

  private ProofOfDelivery proofOfDelivery = new ProofOfDeliveryDataBuilder().build();
  private Pageable pageable = new PageRequest(0, 10);

  @Before
  public void setUp() {
    given(proofOfDeliveryRepository.findOne(proofOfDelivery.getId())).willReturn(proofOfDelivery);
    given(proofOfDeliveryRepository.exists(proofOfDelivery.getId())).willReturn(true);
    given(proofOfDeliveryRepository.save(any(ProofOfDelivery.class)))
        .willAnswer(new SaveAnswer<>());
    given(proofOfDeliveryService.search(
        any(UUID.class),
        any(UUID.class),
        any(Pageable.class)))
        .willReturn(Pagination.getPage(singletonList(proofOfDelivery), pageable, 1));
    given(shipmentRepository.findOne(proofOfDelivery.getShipment().getId()))
        .willReturn(proofOfDelivery.getShipment());

    given(permissionService.getPermissionStrings(INITIAL_USER_ID))
        .willReturn(permissionStringsHandler);

    given(permissionStringsHandler.get())
        .willReturn(ImmutableSet.of(PermissionStringDto.create(
            PODS_MANAGE, proofOfDelivery.getReceivingFacilityId(), proofOfDelivery.getProgramId()
        )));
  }

  @Test
  public void shouldGetAllProofsOfDelivery() {
    PageDto response = restAssured.given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .queryParam(PARAM_PAGE, pageable.getPageNumber())
        .queryParam(PARAM_SIZE, pageable.getPageSize())
        .when()
        .get(RESOURCE_URL)
        .then()
        .statusCode(200)
        .extract()
        .as(PageDto.class);

    assertTrue(response.getContent().iterator().hasNext());
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
    verify(proofOfDeliveryService).search(null, null, pageable);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowPaginationWithZeroSize() {
    Pageable page = new PageRequest(0, 0);
    restAssured.given()
            .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam(PARAM_PAGE, page.getPageNumber())
            .queryParam(PARAM_SIZE, page.getPageSize())
            .when()
            .get(RESOURCE_URL)
            .then()
            .statusCode(400);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowPaginationWithoutSize() {
    Pageable page = new PageRequest(0, 0);
    restAssured.given()
            .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .queryParam(PARAM_PAGE, page.getPageNumber())
            .when()
            .get(RESOURCE_URL)
            .then()
            .statusCode(400);
  }

  @Test
  public void shouldFindProofOfDeliveryBasedOnShipment() {
    PageDto response = restAssured.given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .contentType(APPLICATION_JSON_VALUE)
        .queryParam("shipmentId", proofOfDelivery.getShipment().getId())
        .queryParam(PARAM_PAGE, pageable.getPageNumber())
        .queryParam(PARAM_SIZE, pageable.getPageSize())
        .when()
        .get(RESOURCE_URL)
        .then()
        .statusCode(200)
        .extract()
        .as(PageDto.class);

    assertEquals(0, response.getNumber());
    assertEquals(1, response.getContent().size());
    assertEquals(1, response.getNumberOfElements());
    assertEquals(1, response.getTotalElements());
    assertEquals(1, response.getTotalPages());

    assertEquals(createDto(), getPageContent(response, ProofOfDeliveryDto.class).get(0));
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
    verify(proofOfDeliveryService).search(proofOfDelivery.getShipment().getId(), null, pageable);
  }

  @Test
  public void shouldFindProofOfDeliveryBasedOnOrder() {
    PageDto response = restAssured.given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .contentType(APPLICATION_JSON_VALUE)
        .queryParam("orderId", proofOfDelivery.getShipment().getOrder().getId())
        .queryParam(PARAM_PAGE, pageable.getPageNumber())
        .queryParam(PARAM_SIZE, pageable.getPageSize())
        .when()
        .get(RESOURCE_URL)
        .then()
        .statusCode(200)
        .extract()
        .as(PageDto.class);

    assertEquals(0, response.getNumber());
    assertEquals(1, response.getContent().size());
    assertEquals(1, response.getNumberOfElements());
    assertEquals(1, response.getTotalElements());
    assertEquals(1, response.getTotalPages());

    assertEquals(createDto(), getPageContent(response, ProofOfDeliveryDto.class).get(0));

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
    verify(proofOfDeliveryService)
        .search(null, proofOfDelivery.getShipment().getOrder().getId(), pageable);
  }

  @Test
  public void shouldUpdateProofOfDelivery() {
    String somebody = "Somebody";

    ProofOfDeliveryDto dto = createDto();
    dto.setDeliveredBy(somebody);

    ProofOfDeliveryDto response = restAssured.given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .pathParam("id", proofOfDelivery.getId())
        .body(dto)
        .when()
        .put(ID_URL)
        .then()
        .statusCode(200)
        .extract()
        .as(ProofOfDeliveryDto.class);

    assertThat(response.getDeliveredBy(), is(somebody));
    // Notifications are only sent on POD confirmation
    verify(fulfillmentNotificationService, never())
        .sendPodConfirmedNotification(any(ProofOfDelivery.class));
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldSendNotificationOnProofOfDeliveryConfirmation() {
    ProofOfDeliveryDto dto = createDto();
    dto.setStatus(ProofOfDeliveryStatus.CONFIRMED);

    restAssured.given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .pathParam("id", proofOfDelivery.getId())
        .body(dto)
        .when()
        .put(ID_URL)
        .then()
        .statusCode(200)
        .extract()
        .as(ProofOfDeliveryDto.class);

    verify(fulfillmentNotificationService).sendPodConfirmedNotification(any(ProofOfDelivery.class));
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldNotUpdateProofOfDeliveryIfDoesNotExist() {
    given(proofOfDeliveryRepository.findOne(proofOfDelivery.getId())).willReturn(null);
    given(proofOfDeliveryRepository.exists(proofOfDelivery.getId())).willReturn(false);

    restAssured.given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .pathParam("id", proofOfDelivery.getId())
        .body(createDto())
        .when()
        .put(ID_URL)
        .then()
        .statusCode(404);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldNotUpdateProofOfDeliveryWhenIsSubmitted() {
    proofOfDelivery = new ProofOfDeliveryDataBuilder().buildAsConfirmed();
    setUp();

    String response = restAssured.given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .pathParam("id", proofOfDelivery.getId())
        .body(createDto())
        .when()
        .put(ID_URL)
        .then()
        .statusCode(400)
        .extract()
        .path(MESSAGE_KEY);

    assertThat(response, is(PROOF_OF_DELIVERY_ALREADY_CONFIRMED));
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldRejectUpdateRequestIfUserHasNoRight() {
    denyUserAllRights();

    String response = restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .pathParam("id", proofOfDelivery.getId())
        .body(createDto())
        .when()
        .put(ID_URL)
        .then()
        .statusCode(403)
        .extract().path(MESSAGE_KEY);

    assertThat(response, is(PERMISSION_MISSING));
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldSubmitValidObject() {
    ProofOfDeliveryDto dto = createDto();
    dto.setStatus(ProofOfDeliveryStatus.CONFIRMED);

    ProofOfDeliveryDto response = restAssured.given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .pathParam("id", proofOfDelivery.getId())
        .body(dto)
        .when()
        .put(ID_URL)
        .then()
        .statusCode(200)
        .extract()
        .as(ProofOfDeliveryDto.class);

    assertThat(response.getStatus(), is(ProofOfDeliveryStatus.CONFIRMED));

    ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);

    verify(orderRepository).save(captor.capture());
    verify(stockEventBuilder).fromProofOfDelivery(any(ProofOfDelivery.class));
    verify(stockEventStockManagementService).submit(any(StockEventDto.class));
    assertThat(captor.getValue().getStatus(), is(OrderStatus.RECEIVED));

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldNotSubmitIfObjectIsNotValid() {
    proofOfDelivery = new ProofOfDeliveryDataBuilder().withoutDeliveredBy().build();
    setUp();

    ProofOfDeliveryDto dto = createDto();
    dto.setStatus(ProofOfDeliveryStatus.CONFIRMED);

    String response = restAssured.given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .pathParam("id", proofOfDelivery.getId())
        .body(dto)
        .when()
        .put(ID_URL)
        .then()
        .statusCode(400)
        .extract()
        .path(MESSAGE_KEY);

    verifyZeroInteractions(orderRepository, stockEventBuilder, stockEventStockManagementService);
    assertThat(response, is(MUST_CONTAIN_VALUE));
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldGetChosenProofOfDelivery() {
    ProofOfDeliveryDto response = restAssured.given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .pathParam("id", proofOfDelivery.getId())
        .when()
        .get(ID_URL)
        .then()
        .statusCode(200)
        .extract().as(ProofOfDeliveryDto.class);

    assertTrue(proofOfDeliveryRepository.exists(response.getId()));
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldNotGetNonexistentProofOfDelivery() {
    given(proofOfDeliveryRepository.findOne(proofOfDelivery.getId())).willReturn(null);
    given(proofOfDeliveryRepository.exists(proofOfDelivery.getId())).willReturn(false);

    restAssured.given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .pathParam("id", proofOfDelivery.getId())
        .when()
        .get(ID_URL)
        .then()
        .statusCode(404);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldRejectGetRequestIfUserHasNoRight() {
    denyUserAllRights();

    String response = restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .pathParam("id", proofOfDelivery.getId())
        .when()
        .get(ID_URL)
        .then()
        .statusCode(403)
        .extract()
        .path(MESSAGE_KEY);

    assertThat(response, is(PERMISSIONS_MISSING));
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldPrintProofOfDelivery() {
    JasperReportsMultiFormatView view = mock(JasperReportsMultiFormatView.class);
    given(jasperReportsViewService
        .getJasperReportsView(any(Template.class), any(HttpServletRequest.class)))
        .willReturn(view);

    restAssured.given()
        .pathParam("id", proofOfDelivery.getId())
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .when()
        .get(PRINT_URL)
        .then()
        .statusCode(200);
  }

  @Test
  public void shouldRejectPrintRequestIfUserHasNoRight() {
    denyUserAllRights();

    String response = restAssured
        .given()
        .pathParam("id", proofOfDelivery.getId())
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .when()
        .get(PRINT_URL)
        .then()
        .statusCode(403)
        .extract()
        .path(MESSAGE_KEY);

    assertThat(response, is(PERMISSIONS_MISSING));
    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnNotFoundIfProofOfDeliveryDoesNotExistForAuditLogEndpoint() {
    given(proofOfDeliveryRepository.findOne(any(UUID.class))).willReturn(null);

    restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .pathParam("id", proofOfDelivery.getId())
        .when()
        .get(AUDIT_LOG_URL)
        .then()
        .statusCode(404);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldReturnUnauthorizedIfUserDoesNotHaveRightForAuditLogEndpoint() {
    denyUserAllRights();

    restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .pathParam("id", proofOfDelivery.getId())
        .when()
        .get(AUDIT_LOG_URL)
        .then()
        .statusCode(403);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldGetProofOfDeliveryAuditLog() {
    given(proofOfDeliveryRepository.findOne(any(UUID.class))).willReturn(proofOfDelivery);

    restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .pathParam("id", proofOfDelivery.getId())
        .when()
        .get(AUDIT_LOG_URL)
        .then()
        .statusCode(200);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  @Test
  public void shouldGetProofOfDeliveryAuditLogForAuthorAndChangedPropertyName() {
    given(proofOfDeliveryRepository.findOne(any(UUID.class))).willReturn(proofOfDelivery);

    restAssured
        .given()
        .header(HttpHeaders.AUTHORIZATION, getTokenHeader())
        .pathParam("id", proofOfDelivery.getId())
        .queryParam("author", "admin")
        .queryParam("changedPropertyName", "status")
        .when()
        .get(AUDIT_LOG_URL)
        .then()
        .statusCode(200);

    assertThat(RAML_ASSERT_MESSAGE, restAssured.getLastReport(), RamlMatchers.hasNoViolations());
  }

  private TemplateParameter createParameter(JRParameter jrParameter) {
    TemplateParameter templateParameter = new TemplateParameter();
    templateParameter.setName(jrParameter.getName());
    templateParameter.setDisplayName(jrParameter.getPropertiesMap().getProperty("displayName"));
    templateParameter.setDescription(jrParameter.getDescription());
    templateParameter.setDataType(jrParameter.getValueClassName());

    String selectSql = jrParameter.getPropertiesMap().getProperty("selectSql");
    if (isNotBlank(selectSql)) {
      templateParameter.setSelectSql(selectSql);
    }

    if (jrParameter.getDefaultValueExpression() != null) {
      templateParameter.setDefaultValue(jrParameter.getDefaultValueExpression()
          .getText().replace("\"", "").replace("\'", ""));
    }

    return templateParameter;
  }

  private ProofOfDeliveryDto createDto() {
    ProofOfDeliveryDto dto = new ProofOfDeliveryDto();
    dto.setServiceUrl(serviceUrl);

    proofOfDelivery.export(dto);

    return dto;
  }

}
