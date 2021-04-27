package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.web.model.CustomerDtoPagedList;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    CustomerDtoPagedList listCustomers(Pageable pageable);
}
