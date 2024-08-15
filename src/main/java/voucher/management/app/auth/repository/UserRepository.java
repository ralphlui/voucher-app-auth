package voucher.management.app.auth.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import voucher.management.app.auth.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

	Page<User> findByIsActiveTrue(Pageable pageable);
	
	User save(User user);
	
	User findByEmail(String email);
	
	@Query("SELECT u FROM User u WHERE u.email = ?1 AND u.isActive = ?2")
	public User findByEmailAndStatus(String email, boolean isActive,boolean isVerified);
	
	@Query("SELECT u FROM User u WHERE u.verificationCode = ?1 AND u.isVerified = ?2 AND u.isActive = ?3")
	User findByVerificationCode(String verificationCode,boolean isVerified,boolean isActive);
	
	
}
