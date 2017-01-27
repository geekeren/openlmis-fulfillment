package org.openlmis.fulfillment.service;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_PERMISSION_MISSING;
import static org.openlmis.fulfillment.service.PermissionService.FULFILLMENT_TRANSFER_ORDER;
import static org.openlmis.fulfillment.service.PermissionService.PODS_MANAGE;
import static org.openlmis.fulfillment.service.PermissionService.REQUISITION_CONVERT_TO_ORDER;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.service.referencedata.RightDto;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.service.referencedata.UserReferenceDataService;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.web.MissingPermissionException;

import java.util.UUID;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class PermissionServiceTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Mock
  private UserReferenceDataService userReferenceDataService;

  @Mock
  private AuthenticationHelper authenticationHelper;

  @InjectMocks
  private PermissionService permissionService;

  @Mock
  private UserDto user;

  @Mock
  private RightDto requisitionConvertRight;

  @Mock
  private RightDto fulfillmentTransferOrderRight;

  @Mock
  private RightDto fulfillmentManagePodRight;

  private UUID userId = UUID.randomUUID();
  private UUID requisitionConvertRightId = UUID.randomUUID();
  private UUID fulfillmentTransferOrderRightId = UUID.randomUUID();
  private UUID fulfillmentManagePodRightId = UUID.randomUUID();
  private UUID programId = UUID.randomUUID();
  private UUID facilityId = UUID.randomUUID();
  private Order order =  new Order();
  private ProofOfDelivery proofOfDelivery;

  @Before
  public void setUp() {

    order.setCreatedById(userId);
    order.setProgramId(programId);
    order.setSupplyingFacilityId(facilityId);
    order.setOrderLineItems(Lists.newArrayList());

    proofOfDelivery = new ProofOfDelivery(order);

    when(user.getId()).thenReturn(userId);

    when(requisitionConvertRight.getId()).thenReturn(requisitionConvertRightId);
    when(fulfillmentTransferOrderRight.getId()).thenReturn(fulfillmentTransferOrderRightId);
    when(fulfillmentManagePodRight.getId()).thenReturn(fulfillmentManagePodRightId);

    when(authenticationHelper.getCurrentUser()).thenReturn(user);

    when(authenticationHelper.getRight(REQUISITION_CONVERT_TO_ORDER)).thenReturn(
        requisitionConvertRight);
    when(authenticationHelper.getRight(FULFILLMENT_TRANSFER_ORDER)).thenReturn(
        fulfillmentTransferOrderRight);
    when(authenticationHelper.getRight(PODS_MANAGE)).thenReturn(
        fulfillmentManagePodRight);
  }

  @Test
  public void canConvertToOrder() throws Exception {
    mockFulfillmentHasRight(requisitionConvertRightId, true);

    permissionService.canConvertToOrder(order);

    InOrder order = inOrder(authenticationHelper, userReferenceDataService);
    verifyFulfillmentRight(order, REQUISITION_CONVERT_TO_ORDER, requisitionConvertRightId);
  }

  @Test
  public void cannotConvertToOrder() throws Exception {
    expectException(REQUISITION_CONVERT_TO_ORDER);

    permissionService.canConvertToOrder(order);
  }

  @Test
  public void canTransferOrder() throws Exception {
    mockFulfillmentHasRight(fulfillmentTransferOrderRightId, true);

    permissionService.canTransferOrder(order);

    InOrder order = inOrder(authenticationHelper, userReferenceDataService);
    verifyFulfillmentRight(order, FULFILLMENT_TRANSFER_ORDER, fulfillmentTransferOrderRightId);
  }

  @Test
  public void cannotTransferOrder() throws Exception {
    expectException(FULFILLMENT_TRANSFER_ORDER);

    permissionService.canTransferOrder(order);
  }

  @Test
  public void canManagePod() throws Exception {
    mockFulfillmentHasRight(fulfillmentManagePodRightId, true);

    permissionService.canManagePod(proofOfDelivery);

    InOrder order = inOrder(authenticationHelper, userReferenceDataService);
    verifyFulfillmentRight(order, PODS_MANAGE, fulfillmentManagePodRightId);
  }

  @Test
  public void cannotManagePod() throws Exception {
    expectException(PODS_MANAGE);

    permissionService.canManagePod(proofOfDelivery);
  }

  private void mockFulfillmentHasRight(UUID rightId, boolean assign) {
    ResultDto<Boolean> resultDto = new ResultDto<>(assign);
    when(userReferenceDataService
        .hasRight(userId, rightId, null, null, facilityId)
    ).thenReturn(resultDto);
  }

  private void expectException(String rightName) {
    exception.expect(MissingPermissionException.class);
    exception.expect(hasProperty("params", arrayContaining(rightName)));
    exception.expectMessage(ERROR_PERMISSION_MISSING);
  }

  private void verifyFulfillmentRight(InOrder order, String rightName, UUID rightId) {
    order.verify(authenticationHelper).getCurrentUser();
    order.verify(authenticationHelper).getRight(rightName);
    order.verify(userReferenceDataService).hasRight(userId, rightId, null, null, facilityId);
  }

}

