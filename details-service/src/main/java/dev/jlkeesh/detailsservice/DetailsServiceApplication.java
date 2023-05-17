package dev.jlkeesh.detailsservice;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@EnableDiscoveryClient
public class DetailsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DetailsServiceApplication.class, args);
    }
}


@RestController
@RequestMapping("/api/post-details")
@RequiredArgsConstructor
@Slf4j
class PostDetailsController {
    private final PostDetailsRepository postDetailsRepository;

    @PostMapping
    public PostDetails create(@RequestBody PostDetailsCreateDTO dto) {
        return postDetailsRepository.save(new PostDetails(dto.postId(), dto.body()));
    }
    @GetMapping("/{id}")
    public PostDetails get(@PathVariable Integer id) {
        log.info("Getting PostDetail by post id: {}", id);
        return postDetailsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PostDetails not found: %s".formatted(id)));
    }

}

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
class PostDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer postId;

    private String body;

    public PostDetails(Integer postId, String body) {
        this.postId = postId;
        this.body = body;
    }
}

interface PostDetailsRepository extends JpaRepository<PostDetails, Integer> {
}

record PostDetailsCreateDTO(Integer postId, String body){}