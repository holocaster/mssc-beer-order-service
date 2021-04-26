package guru.sfg.beer.order.service.listerners;

import br.com.prcompany.beerevents.events.AllocateFailRequest;
import br.com.prcompany.beerevents.utils.EventsConstants;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class AllocationFailListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = EventsConstants.ALLOCATE_FAIL_QUEUE)
    public void listen(@Payload AllocateFailRequest request) {
        log.info("Receiving notification for allocation failed, {}", request.toString());
    }
}
