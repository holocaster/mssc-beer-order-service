package guru.sfg.beer.order.service.listerners;

import br.com.prcompany.beerevents.events.AllocateOrderResult;
import br.com.prcompany.beerevents.model.enums.BeerOrderEventEnum;
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
public class AllocationOrderResultListener {

    private BeerOrderManager beerOrderManager;

    @JmsListener(destination = EventsConstants.ALLOCATE_ORDER_RESULT_QUEUE)
    public void listen(@Payload AllocateOrderResult result) {
        if (!result.isAllocationError() && !result.isPendingInventory()) {
            //allocated normally
            this.beerOrderManager.beerOrderAllocation(result.getBeerOrderDTO(), BeerOrderEventEnum.ALLOCATION_SUCCESS);
        } else if (!result.isPendingInventory() && result.isPendingInventory()) {
            //pending inventory
            this.beerOrderManager.beerOrderAllocation(result.getBeerOrderDTO(), BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);
        } else if (result.isAllocationError()) {
            //allocation error
            this.beerOrderManager.beerOrderAllocation(result.getBeerOrderDTO(), BeerOrderEventEnum.ALLOCATION_FAILED);
        }
    }
}
