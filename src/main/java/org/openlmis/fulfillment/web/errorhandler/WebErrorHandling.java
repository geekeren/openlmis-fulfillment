package org.openlmis.fulfillment.web.errorhandler;

import org.openlmis.fulfillment.util.Message;
import org.openlmis.fulfillment.web.InvalidOrderStatusException;
import org.openlmis.fulfillment.web.MissingPermissionException;
import org.openlmis.fulfillment.web.OrderNotFoundException;
import org.openlmis.fulfillment.web.ProofOfDeliveryNotFoundException;
import org.openlmis.fulfillment.web.ProofOfDeliverySubmitException;
import org.openlmis.fulfillment.web.validator.ValidationException;
import org.openlmis.fulfillment.web.validator.ValidationMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Controller advice responsible for handling errors from web layer.
 */
@ControllerAdvice
public class WebErrorHandling extends AbstractErrorHandling {

  @ExceptionHandler(MissingPermissionException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ResponseBody
  public Message.LocalizedMessage handleMissingPermissionException(MissingPermissionException ex) {
    return logErrorAndRespond("Missing permission for this action", ex);
  }

  @ExceptionHandler(InvalidOrderStatusException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public Message.LocalizedMessage handleInvalidOrderStatusException(
      InvalidOrderStatusException ex) {
    return logErrorAndRespond("Cannot update status for this order", ex);
  }

  @ExceptionHandler(OrderNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public Message.LocalizedMessage handleOrderNotFoundException(OrderNotFoundException ex) {
    return logErrorAndRespond("Cannot find an order", ex);
  }

  @ExceptionHandler(ProofOfDeliveryNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ResponseBody
  public Message.LocalizedMessage handleProofOfDeliveryNotFoundException(
      ProofOfDeliveryNotFoundException ex) {
    return logErrorAndRespond("Cannot find a proof od delivery", ex);
  }

  @ExceptionHandler(ProofOfDeliverySubmitException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public Message.LocalizedMessage handleProofOfDeliverySubmitException(
      ProofOfDeliverySubmitException ex) {
    return logErrorAndRespond("Cannot submit a proof od delivery", ex);
  }

  @ExceptionHandler(ValidationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public ValidationMessage handleValidationException(ValidationException ex) {
    return new ValidationMessage(getLocalizedMessage(ex), ex.getDetails());
  }

}
