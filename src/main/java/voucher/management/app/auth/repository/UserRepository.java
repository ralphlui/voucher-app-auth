package voucher.management.app.auth.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import voucher.management.app.auth.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

	Page<User> findByIsActiveTrue(Pageable pageable);
	
	User save(User user);
	
	User findByEmail(String email);
}
