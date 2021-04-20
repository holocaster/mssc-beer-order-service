package guru.sfg.beer.order.service.listerners;

import br.com.prcompany.beerevents.events.ValidateBeerOrderResult;
import br.com.prcompany.beerevents.utils.EventsConstants;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ValidateOrderResultListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = EventsConstants.VALIDATE_ORDER_RESULT_QUEUE)
    public void listenOrderResult(@Payload ValidateBeerOrderResult validateBeerOrderResult) {
        final UUID beerOrderId = validateBeerOrderResult.getOrderId();

        log.debug("Validation result for id: {}" , beerOrderId);

        this.beerOrderManager.processValidationResult(beerOrderId, validateBeerOrderResult.getIsValid());
    }
}
