
package com.work.matmode.processor.web;

import com.work.matmode.processor.model.Result;
import com.work.matmode.processor.repository.ResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/result")
@RequiredArgsConstructor
@CrossOrigin
public class CrudController {

    private final ResultRepository resultRepository;


    @GetMapping
    public ResponseEntity<List<Result>> getAllResults() {
        List<Result> results = resultRepository.findAll();
        return ResponseEntity.ok(results);
    }


    @GetMapping("/{id}")
    public ResponseEntity<Result> getResultById(@PathVariable String id) {
        Optional<Result> result = resultRepository.findById(id);
        return result.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }


    @PostMapping
    public ResponseEntity<Result> createResult(@RequestBody Result result) {
        Result savedResult = resultRepository.save(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedResult);
    }


    @PutMapping("/{id}")
    public ResponseEntity<Result> updateResult(@PathVariable String id, @RequestBody Result updatedResult) {
        Optional<Result> existingResult = resultRepository.findById(id);
        if (existingResult.isPresent()) {
            updatedResult.setId(id);
            Result savedResult = resultRepository.save(updatedResult);
            return ResponseEntity.ok(savedResult);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResult(@PathVariable String id) {
        if (resultRepository.existsById(id)) {
            resultRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
