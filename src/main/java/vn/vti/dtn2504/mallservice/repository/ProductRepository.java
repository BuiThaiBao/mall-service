package vn.vti.dtn2504.mallservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.vti.dtn2504.mallservice.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
