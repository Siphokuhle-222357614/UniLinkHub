package za.co.unilinkhub.media.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import za.co.unilinkhub.common.exception.UnauthorizedException;
import za.co.unilinkhub.common.web.RejectionMessages;
import za.co.unilinkhub.media.application.ImageService;
import za.co.unilinkhub.security.CurrentUserProvider;
import za.co.unilinkhub.security.StudentOnly;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping(value = "/api/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @StudentOnly("upload listing photos")
    public ImageService.UploadedImage upload(@RequestParam("file") MultipartFile file) {
        return imageService.upload(CurrentUserProvider.current()
                .orElseThrow(() -> new UnauthorizedException(RejectionMessages.NOT_LOGGED_IN)), file);
    }

    // Each upload gets a new id and is never modified, so browsers can cache it for a year.
    @GetMapping("/api/images/{id}")
    public ResponseEntity<byte[]> get(@PathVariable UUID id) {
        ImageService.ImageContent image = imageService.load(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(image.data());
    }
}
