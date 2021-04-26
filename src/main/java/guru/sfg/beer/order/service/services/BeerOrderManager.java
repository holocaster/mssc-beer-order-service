package guru.sfg.beer.order.service.services;

import br.com.prcompany.beerevents.model.BeerOrderDTO;
import br.com.prcompany.beerevents.model.enums.BeerOrderEventEnum;
import br.com.prcompany.beerevents.model.enums.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.domain.BeerOrder;

import java.util.UUID;

public interface BeerOrderManager {

    BeerOrder newBeerOrder(BeerOrder beerOrder);

    void processValidationResult(UUID beerOrderId, Boolean isValid);

    void beerOrderAllocation(BeerOrderDTO beerOrderDTO, BeerOrderEventEnum beerOrderEventEnum, BeerOrderStatusEnum beerOrderStatusEnum);

    void beerOrderPickedUp(UUID id);

    void cancelOrder(UUID uuid);
}
