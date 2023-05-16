package dev.jlkeesh.postservice;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.client.RestTemplateBuilderConfigurer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@SpringBootApplication
@EnableDiscoveryClient
@EnableHystrix
public class PostServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostServiceApplication.class, args);
    }

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }
}


@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
class PostController {
    private final PostRepository postRepository;
    private final RestTemplate restTemplate;
    private final PostDetailsClient postDetailsClient;
    private final CommentClient commentClient;

    @GetMapping
    public List<Post> getAll() {
        return postRepository.findAll();
    }

    @GetMapping("/{id}")
    public Post get(@PathVariable Integer id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found: %s".formatted(id)));
    }

    @GetMapping("/{id}/details")
    public PostDetail getDetail(@PathVariable Integer id) {
        var post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found: %s".formatted(id)));
        return new PostDetail(post.getId(),
                post.getTitle(),
                post.getSummary(),
                postDetailsClient.get(post.getId()),
                commentClient.getAllByPostId(post.getId())
        );
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        postRepository.deleteById(id);
    }

    @PostMapping
    public Post create(@RequestBody PostCreateDTO dto) {
        var post = postRepository.save(new Post(dto.title(), dto.summary()));
        postDetailsClient.create(dto);
        return post;
    }


}


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String title;
    private String summary;

    public Post(String title, String summary) {
        this.title = title;
        this.summary = summary;
    }
}

interface PostRepository extends JpaRepository<Post, Integer> {
}

record PostCreateDTO(String title, String summary, String body) {
}

record PostDetail(Integer id, String title, String summary, Object body, List<Comment> postComments) {
}

record PostDetailsCreateDTO(Integer postId, String body) {
}

record Comment(Integer id, String message, Integer postId) {
}

@Service
@RequiredArgsConstructor
class PostDetailsClient {
    private final RestTemplate restTemplate;
    @Value(value = "${post.details.service.details}")
    private String url;
    @Value(value = "${post.details.service.baseUrl}")
    private String createUrl;

    public Object get(Integer postId) {
        return restTemplate.getForObject(url, Object.class, postId);
    }

    public void create(PostCreateDTO dto) {
        restTemplate.postForObject(createUrl, dto, Object.class);
    }
}

@Service
@RequiredArgsConstructor
class CommentClient {

    private final RestTemplate restTemplate;

    @Value(value = "${comment.service.commentsBypostID}")
    private String url;


    @HystrixCommand(fallbackMethod = "getAllByPostIdFallback")
    public List<Comment> getAllByPostId(Integer postId) {
        return restTemplate.exchange(url,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Comment>>() {
                },
                postId).getBody();
    }

    public List<Comment> getAllByPostIdFallback(Integer postId) {
        return Collections.emptyList();
    }
}