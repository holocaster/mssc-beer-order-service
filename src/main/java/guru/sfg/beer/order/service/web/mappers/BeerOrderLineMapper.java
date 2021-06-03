package guru.sfg.beer.order.service.web.mappers;

import br.com.prcompany.beerevents.model.BeerOrderLineDTO;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;

@Mapper(uses = {DateMapper.class})
@DecoratedWith(BeerOrderLineMapperDecorator.class)
public interface BeerOrderLineMapper {

    BeerOrderLineDTO beerOrderLineToDto(BeerOrderLine line);

    BeerOrderLine dtoToBeerOrderLine(BeerOrderLineDTO dto);
}
