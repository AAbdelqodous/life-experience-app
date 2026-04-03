package com.maintainance.service_center;

import com.maintainance.service_center.role.Role;
import com.maintainance.service_center.role.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class ServiceCenterApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceCenterApplication.class, args);
	}

    @Bean
    public CommandLineRunner runner(RoleRepository roleRepository){
        return args -> {
            if (roleRepository.findByName("USER").isEmpty()){
                roleRepository.save(
                        Role.builder()
                                .name("USER")
                                .build()
                );
            }
        };
    }
}
