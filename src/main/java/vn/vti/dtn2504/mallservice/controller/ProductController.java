package vn.vti.dtn2504.mallservice.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.vti.dtn2504.mallservice.dto.request.CreateProductRequest;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/products")
@RequiredArgsConstructor
public class ProductController {


    @PostMapping
    public ResponseEntity<String> createProduct(@RequestBody CreateProductRequest request) {
        log.info("Received request to create product with payload: {}", request);
        try {

            log.info("Successfully processed createProduct request.");
            return ResponseEntity.ok("hello");
        } catch (Exception e) {
            log.error("Failed to create product. Error: {}", e.getMessage(), e);

            throw e;
        }
    }
}
