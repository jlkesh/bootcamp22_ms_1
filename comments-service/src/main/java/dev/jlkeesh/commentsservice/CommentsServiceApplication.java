package dev.jlkeesh.commentsservice;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.GET;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class CommentsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommentsServiceApplication.class, args);
    }

}


@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
class CommentController {

    private final CommentRepository commentRepository;

    @PostMapping
    public Comment create(@RequestBody CommentCreateDTO dto) {
        Comment comment = Comment.builder()
                .message(dto.message())
                .postId(dto.postId())
                .build();
        return commentRepository.save(comment);
    }

    @GetMapping("/{postId}/post")
    public List<Comment> getAllByPostID(@PathVariable Integer postId) throws InterruptedException {
        TimeUnit.SECONDS.sleep(2);
        return commentRepository.findAllByPostID(postId);
    }

    @GetMapping("/{id}")
    public Comment get(@PathVariable Integer id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + id));
    }
}

interface CommentRepository extends JpaRepository<Comment, Integer> {
    @Query("select t from Comment t where t.postId = :postId")
    List<Comment> findAllByPostID(@NonNull @Param("postId") Integer id);
}

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private Integer postId;
}

record CommentCreateDTO(@NotBlank String message, @Positive Integer postId) {
}
