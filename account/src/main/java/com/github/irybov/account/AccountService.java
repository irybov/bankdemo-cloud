package com.github.irybov.account;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.data.util.Streamable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.irybov.shared.AccountDTO;
import com.github.irybov.shared.BillDTO;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true, noRollbackFor = Exception.class)
@RequiredArgsConstructor
public class AccountService {
	
	private final Environment env;
//    private final Validator validator;
    private final BillClient billClient;
//	private final RestTemplate restTemplate;
	private final AccountMapper mapStruct;
	private final AccountJDBC jdbc;
	
	@Lazy
	@Autowired
	private AccountService self;
	
	private static final String ACCOUNTS = "accounts";
	
	@Transactional
	public void create(Registration registration) {
		
//		Set<ConstraintViolation<Registration>> violations = validator.validate(registration);
//		if(violations.isEmpty()) {
			Account account = mapStruct.toDB(registration);
//			account.setCreatedAt(Timestamp.valueOf(OffsetDateTime.now()
//				.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()));
//			account.setActive(true);
//			account.setRoles(Collections.singleton(Role.CLIENT.getName()));
			self.update(account);
//		}
	};
	
	public String generateToken(String header) {
		
		String[] login = header.split(":");
		Account account = self.getAccount(login[0]);
		if(account.getPassword().equals(login[1])) {
			SecretKey key = new SecretKeySpec(env.getProperty("token.secret").getBytes(), 
					SignatureAlgorithm.HS256.getJcaName());
//					Jwts.SIG.HS256);
			Instant now = Instant.now();
			String token = Jwts.builder()
					.claim("scope", account.getRoles())
					.claim("active", account.isActive())
					.subject(account.getPhone())
					.issuer("bankdemo")
					.expiration(Date.from(now.plusSeconds(
							Integer.parseInt(env.getProperty("token.lifetime")))))
					.issuedAt(Date.from(now))
					.signWith(key, Jwts.SIG.HS256)
					.compact();
			return token;
		}
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong password provided");
	};
	
//	public AccountDTO getOne(int id) {return mapStruct.toDTO(jdbc.findById(id).get());}
	
	public AccountDTO getOne(String phone) {
		
		Account account = self.getAccount(phone);
//		List<BillDTO> bills = billClient.getList(account.getId());
		Set<BillDTO> bills = billClient.getList(account.getId());
//		ResponseEntity<List<BillDTO>> response = 
//				restTemplate.exchange("http://BILL/bills/" + account.getId() + "/list", 
//				HttpMethod.GET, null, new ParameterizedTypeReference<List<BillDTO>>(){});
		
		AccountDTO dto = mapStruct.toDTO(account);
//		dto.setBills(new HashSet<>(bills));
		dto.setBills(bills);
		return dto;
	}
	
	public List<AccountDTO> getAll() {
		return mapStruct.toList(Streamable.of(jdbc.findAll()).toList());
	}
	
	public void changePassword(String phone, String password) {
		
		Account account = self.getAccount(phone);
		account.setPassword(password);
		self.update(account);
	}
	
	public BillDTO addBill(String phone, String currency) {
		
		Account account = self.getAccount(phone);
		BillDTO bill = billClient.create(currency, account.getId());
//        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("http://BILL/bills")
//    	        .queryParam("currency", currency)
//    	        .queryParam("owner", account.getId());
//        BillDTO bill = restTemplate.postForObject(uriBuilder.toUriString(), null, BillDTO.class);
        
        if(account.getBills() != null) {account.getBills().add(bill.getId());}
        else {account.setBills(Stream.of(bill.getId()).collect(Collectors.toSet()));}
        self.update(account);
        return bill;
	}
	
	public void deleteBill(String phone, int id) {
		
		Account account = self.getAccount(phone);
		billClient.delete(id);
		account.getBills().remove(id);
		self.update(account);
	}
	
	@Cacheable(cacheNames = ACCOUNTS, key = "#phone", sync = true)
	public Account getAccount(String phone) {
		
		Account account = jdbc.findByPhone(phone);
		if(account != null) return account;
		throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
				String.format("Account with phone %s not found ", phone));
	}
	
	@Transactional(propagation = Propagation.MANDATORY)
	@CachePut(cacheNames = ACCOUNTS, key = "#result.phone", unless = "#result == null")
	public Account update(Account account) {
		jdbc.save(account);
		return account;
	}
/*	
	boolean checkFraud(String phone, String header) {
		
		String jwt = header.replace("Bearer", "").trim();
		
		String tokenSecret = env.getProperty("token.secret");
		byte[] secretKeyBytes = tokenSecret.getBytes();
		SecretKey secretKey = Keys.hmacShaKeyFor(secretKeyBytes);
		
		JwtParser parser = Jwts.parser()
                .verifyWith(secretKey)
                .build();
		Jws<Claims> parsedToken = 
				parser.parseSignedClaims(jwt);		
		String owner = parsedToken.getPayload().getSubject();
		
		if(owner.equals(phone)) return true;
		return false;
	}
*/	
}
