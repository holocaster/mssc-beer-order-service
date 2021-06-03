package guru.sfg.beer.order.service.web.model;

import br.com.prcompany.beerevents.model.CustomerDto;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class CustomerDtoPagedList extends PageImpl<CustomerDto> {
    public CustomerDtoPagedList(List<CustomerDto> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }

    public CustomerDtoPagedList(List<CustomerDto> content) {
        super(content);
    }
}
