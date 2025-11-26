package vn.vti.dtn2504.mallservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.vti.dtn2504.mallservice.entity.Order;

public interface OrderRepository extends JpaRepository<Order, String> {
}
