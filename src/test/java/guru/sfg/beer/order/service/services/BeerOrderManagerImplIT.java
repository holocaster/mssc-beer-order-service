package guru.sfg.beer.order.service.services;

import br.com.prcompany.beerevents.events.DeallocateOrderRequest;
import br.com.prcompany.beerevents.model.BeerDTO;
import br.com.prcompany.beerevents.model.BeerStyleEnum;
import br.com.prcompany.beerevents.model.enums.BeerOrderStatusEnum;
import br.com.prcompany.beerevents.utils.EventsConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import guru.sfg.beer.order.service.BeerOrderServiceApplication;
import guru.sfg.beer.order.service.WireMockInitializer;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;


@ContextConfiguration(initializers = { WireMockInitializer.class }, classes = {
        BeerOrderServiceApplication.class })
@SpringBootTest
public class BeerOrderManagerImplIT {

    public static final String FAIL_VALIDATION = "fail-validation";
    public static final String FAIL_ALLOCATION = "fail-allocation";
    public static final String PARTIAL_ALLOCATION = "partial-allocation";
    public static final String DONT_VALIDATE = "dont-validate";
    public static final String DONT_ALLOCATE = "dont-allocate";

    Customer testCustomer;
    UUID beerID = UUID.randomUUID();
    BeerOrder beerOrder;

    @Autowired
    BeerOrderManager beerOrderManager;

    @Autowired
    BeerOrderRepository beerOrderRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    WireMockServer wireMockServer;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Value("${beer_upc_path_v1}")
    private String beerUpcPathV1;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        this.testCustomer = this.customerRepository.save(Customer.builder()
                .customerName("Test Customer")
                .build());
        BeerDTO beerDTO = BeerDTO.builder().id(beerID).upc("12345").beerStyle(BeerStyleEnum.ALE).build();

        this.wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo(this.beerUpcPathV1+ "null")).willReturn(
                WireMock.okJson(this.objectMapper.writeValueAsString(beerDTO))));

        this.beerOrder = this.createBeerOrder();
    }

    @AfterEach
    public void afterEach()
    {
        this.wireMockServer.resetAll();
    }

    private void waitUntil(BeerOrderStatusEnum beerOrderStatusEnum) {
        await().untilAsserted(() -> {
            BeerOrder foundOrder = this.beerOrderRepository.findById(this.beerOrder.getId()).get();
            Assertions.assertEquals(beerOrderStatusEnum, foundOrder.getOrderStatus());
        });
    }

    @Test
    void testAllocatedToCancel() {
        BeerOrder savedBeerOrder = this.beerOrderManager.newBeerOrder(beerOrder);

        this.waitUntil(BeerOrderStatusEnum.ALLOCATED);

        this.beerOrderManager.cancelOrder(savedBeerOrder.getId());

        this.waitUntil(BeerOrderStatusEnum.CANCELED);

        final DeallocateOrderRequest deallocateOrderRequest = (DeallocateOrderRequest) this.jmsTemplate.receiveAndConvert(EventsConstants.DEALLOCATE_ORDER_QUEUE);
        Assertions.assertNotNull(deallocateOrderRequest);
        Assertions.assertEquals(deallocateOrderRequest.getBeerOrderDTO().getId(), savedBeerOrder.getId());
    }

    @Test
    void testValidationPendingToCancel() {
        this.beerOrder.setCustomerRef(DONT_VALIDATE);

        BeerOrder savedBeerOrder = this.beerOrderManager.newBeerOrder(beerOrder);

        this.waitUntil(BeerOrderStatusEnum.VALIDATION_PENDING);

        this.beerOrderManager.cancelOrder(savedBeerOrder.getId());

        this.waitUntil(BeerOrderStatusEnum.CANCELED);
    }

    @Test
    void testAllocationPendingToCancel() {
        this.beerOrder.setCustomerRef(DONT_ALLOCATE);

        BeerOrder savedBeerOrder = this.beerOrderManager.newBeerOrder(beerOrder);

        this.waitUntil(BeerOrderStatusEnum.ALLOCATION_PENDING);

        this.beerOrderManager.cancelOrder(savedBeerOrder.getId());

        this.waitUntil(BeerOrderStatusEnum.CANCELED);
    }


    @Test
    void testFailedAllocation() {
        this.beerOrder.setCustomerRef(FAIL_ALLOCATION);

        BeerOrder savedBeerOrder = this.beerOrderManager.newBeerOrder(beerOrder);

        this.waitUntil(BeerOrderStatusEnum.ALLOCATION_EXCEPTION);

        savedBeerOrder = this.beerOrderRepository.findById(savedBeerOrder.getId()).get();

//        final AllocateFailRequest allocateFailRequest = (AllocateFailRequest) this.jmsTemplate.receiveAndConvert(EventsConstants.ALLOCATE_FAIL_QUEUE);
//        Assertions.assertNotNull(allocateFailRequest);
//        Assertions.assertEquals(allocateFailRequest.getUuid(), savedBeerOrder.getId());
        Assertions.assertNotNull(savedBeerOrder);
        Assertions.assertEquals(BeerOrderStatusEnum.ALLOCATION_EXCEPTION, savedBeerOrder.getOrderStatus());
    }

    @Test
    void testPartialAllocation() {

        this.beerOrder.setCustomerRef(PARTIAL_ALLOCATION);

        BeerOrder savedBeerOrder = this.beerOrderManager.newBeerOrder(beerOrder);

        this.waitUntil(BeerOrderStatusEnum.PENDING_INVENTORY);

        savedBeerOrder = this.beerOrderRepository.findById(savedBeerOrder.getId()).get();

        Assertions.assertNotNull(savedBeerOrder);
        Assertions.assertEquals(BeerOrderStatusEnum.PENDING_INVENTORY, savedBeerOrder.getOrderStatus());
    }

    @Test
    void testFailedValidation() throws JsonProcessingException {
        this.beerOrder.setCustomerRef(FAIL_VALIDATION);

        BeerOrder savedBeerOrder = this.beerOrderManager.newBeerOrder(beerOrder);

        this.waitUntil(BeerOrderStatusEnum.VALIDATION_EXCEPTION);

        savedBeerOrder = this.beerOrderRepository.findById(savedBeerOrder.getId()).get();

        Assertions.assertNotNull(savedBeerOrder);
        Assertions.assertEquals(BeerOrderStatusEnum.VALIDATION_EXCEPTION, savedBeerOrder.getOrderStatus());
    }

    @Test
    void testNewToAllocated() throws JsonProcessingException {
        this.beerOrder = this.createBeerOrder();

        BeerOrder savedBeerOrder = this.beerOrderManager.newBeerOrder(beerOrder);

        this.waitUntil(BeerOrderStatusEnum.ALLOCATED);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = this.beerOrderRepository.findById(beerOrder.getId()).get();
            BeerOrderLine line = foundOrder.getBeerOrderLines().iterator().next();
            Assertions.assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
        });

        savedBeerOrder = this.beerOrderRepository.findById(savedBeerOrder.getId()).get();

        Assertions.assertNotNull(savedBeerOrder);
        Assertions.assertEquals(BeerOrderStatusEnum.ALLOCATED, savedBeerOrder.getOrderStatus());
    }

    @Test
    void testNewToPickedUp() throws JsonProcessingException {
        this.beerOrder = this.createBeerOrder();

        BeerOrder savedBeerOrder = this.beerOrderManager.newBeerOrder(beerOrder);

        this.waitUntil(BeerOrderStatusEnum.ALLOCATED);

        this.beerOrderManager.beerOrderPickedUp(savedBeerOrder.getId());

        this.waitUntil(BeerOrderStatusEnum.PICKED_UP);

        BeerOrder pickedUpOrder = this.beerOrderRepository.findById(beerOrder.getId()).get();

        Assertions.assertNotNull(pickedUpOrder);
        Assertions.assertEquals(BeerOrderStatusEnum.PICKED_UP, pickedUpOrder.getOrderStatus());
    }

    public BeerOrder createBeerOrder() {
        BeerOrder beerOrder = BeerOrder.builder()
                .customer(this.testCustomer)
                .build();

        Set<BeerOrderLine> beerOrderLines = new HashSet<>();
        beerOrderLines.add(BeerOrderLine.builder().beerId(this.beerID).orderQuantity(1).beerOrder(beerOrder).build());
        beerOrder.setBeerOrderLines(beerOrderLines);

        return beerOrder;
    }

 }
