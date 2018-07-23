package com.github.achuzhmarov.tutorial.product;

import com.github.achuzhmarov.tutorial.common.exception.DataNotFoundException;
import com.github.achuzhmarov.tutorial.user.Customer;
import com.github.achuzhmarov.tutorial.user.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    public ProductService(ProductRepository productRepository,
                          CustomerRepository customerRepository) {
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new DataNotFoundException("Product", productId));
    }

    @Transactional
    public Product createProduct(Product product) {
        return productRepository.save(new Product(product));
    }

    @Transactional
    public Product updateProduct(Long productId, Product product) {
        Product dbProduct = productRepository.findById(productId)
                .orElseThrow(() -> new DataNotFoundException("Product", productId));

        dbProduct.setPrice(product.getPrice());
        dbProduct.setDiscount(product.getDiscount());
        dbProduct.setName(product.getName());
        dbProduct.setIsAdvertised(product.isAdvertised());

        return productRepository.save(dbProduct);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateBonusPoints(String customerLogin, Map<Long, Long> productQuantities) {
        List<Product> products = productRepository.findAllById(productQuantities.keySet());
        Customer customer = customerRepository.findByLogin(customerLogin)
                .orElseThrow(() -> new DataNotFoundException("Customer", customerLogin));

        return products.stream()
            .map(p -> calculateBonusPoints(customer, p, productQuantities.get(p.getId())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateBonusPoints(Customer user, Product product, Long quantity) {
        if (product.getDiscount() != null) {
            return BigDecimal.ZERO;
        }

        BigDecimal resultMultiplier = calculateMultipliers(user, product).stream()
            .sorted(Comparator.reverseOrder())
            .limit(2)
            .reduce(BigDecimal.ONE, BigDecimal::multiply);

        return product.getPrice()
            .multiply(new BigDecimal(quantity))
            .multiply(resultMultiplier)
            .divide(BigDecimal.TEN, RoundingMode.HALF_UP);
    }

    private List<BigDecimal> calculateMultipliers(Customer user, Product product) {
        List<BigDecimal> multipliers = new ArrayList<>();

        if (user.getFavProduct().equals(product)) {
            if (user.isPremium()) {
                multipliers.add(new BigDecimal(8));
            } else {
                multipliers.add(new BigDecimal(5));
            }
        } else if (user.isPremium()) {
            multipliers.add(new BigDecimal(2));
        }

        if (product.isAdvertised()) {
            multipliers.add(new BigDecimal(3));
        }

        if (product.getPrice().compareTo(new BigDecimal(10000)) > 0) {
            multipliers.add(new BigDecimal(4));
        }

        return multipliers;
    }
}