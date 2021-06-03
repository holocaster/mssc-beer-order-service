package guru.sfg.beer.order.service.services.beer;

import br.com.prcompany.beerevents.model.BeerDTO;

import java.util.Optional;
import java.util.UUID;

public interface BeerService {

    Optional<BeerDTO> getBeerById(UUID uuid);

    Optional<BeerDTO> getBeerByUpc(String upc);
}
