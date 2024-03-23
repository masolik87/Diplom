package com.clothingstore.ClothingStoreResourceServer.controllers;

import com.clothingstore.ClothingStoreResourceServer.models.Order;
import com.clothingstore.ClothingStoreResourceServer.models.Product;
import com.clothingstore.ClothingStoreResourceServer.models.ProductInOrderDetails;
import com.clothingstore.ClothingStoreResourceServer.services.FacadeService;
import com.clothingstore.ClothingStoreResourceServer.services.OrderService;
import com.clothingstore.ClothingStoreResourceServer.services.ProductInOrderDetailsService;
import com.clothingstore.ClothingStoreResourceServer.services.ProductService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/order")
@Timed(value = "create_order_time", description = "Time taken to create an order")
@CrossOrigin(origins = "http://localhost:8080")
public class OrderController {
    @Autowired
    private final FacadeService facadeService;

    @Counted(value = "createOrder.count", description = "Counts how many orders are created")
    @PostMapping
    public ResponseEntity<Void> createOrder(@RequestBody Map<String, Object> requestData) {
        // Парсим json с формы клиента и получаем: map с ключом = id продукта и значением = количеству заказываемого продукта, username пользователя
        Map<Long, Integer> productsOrderAmount = new HashMap<>();
        List<Map<Long, Object>> productsList = (List<Map<Long, Object>>) requestData.get("products");
        productsList.forEach(product -> {
            Long productId = ((Integer) product.get("id")).longValue();
            int amount = (int) product.get("amount");
            productsOrderAmount.put(productId, amount);
        });

        String customerUsername = (String) requestData.get("username");

        // Создаем экземпляр order и помещаем пока что только имя пользователя
        Order order = new Order();
        order.setCustomerUsername(customerUsername);
        facadeService.saveOrder(order);

        // Извлекаем из map id продуктов и их количество, помещаем в лист ProductInOrderDetails,
        // который привязан к order и хранит в себе: ссылку на order, ссылку на product, количество заказанного товара
        List<ProductInOrderDetails> productInOrderDetails = new ArrayList<>();
        productsOrderAmount.forEach((key, value) -> {
            ProductInOrderDetails details = new ProductInOrderDetails(
                    order,
                    facadeService.getProductById(key),
                    value
            );
            productInOrderDetails.add(details);
            System.out.println(details.toString());
            facadeService.saveProductInOrderDetails(details);
        });

        // Связываем order с множеством ProductInOrderDetails
        order.setProductInOrderDetails(productInOrderDetails);

        facadeService.saveOrder(order);

        // Запускаем процесс оплаты
        facadeService.startOrderPayment(order);

        return ResponseEntity.ok().build();
    }
}
