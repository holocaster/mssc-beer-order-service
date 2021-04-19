package guru.sfg.beer.order.service.domain.enums;

public enum BeerOrderEventEnum {
    VALIDATION_ERROR, VALIDATION_PASSED, VALIDATION_FAILED,
    ALLOCATION_SUCCESS, ALLOCATION_NO_INVENTORY, ALLOCATION_FAILED,
    BEER_ORDER_PICKED_UP

}