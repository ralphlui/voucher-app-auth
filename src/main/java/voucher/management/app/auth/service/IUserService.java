package voucher.management.app.auth.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;

import voucher.management.app.auth.dto.UserDTO;

public interface IUserService {
	Map<Long, List<UserDTO>> findByIsActiveTrue(Pageable pageable);
}
