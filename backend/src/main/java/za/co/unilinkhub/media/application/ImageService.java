package za.co.unilinkhub.media.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.common.exception.ForbiddenException;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.common.web.RejectionMessages;
import za.co.unilinkhub.media.domain.StoredImage;
import za.co.unilinkhub.media.repository.ImageRepository;
import za.co.unilinkhub.security.UserPrincipal;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ImageService {

    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final String URL_PREFIX = "/api/images/";
    private static final Pattern OWN_IMAGE_URL = Pattern.compile("^/api/images/[0-9a-fA-F-]{36}$");

    private final ImageRepository imageRepository;

    public record UploadedImage(UUID id, String url, String contentType, int sizeBytes) {
    }

    public record ImageContent(String contentType, byte[] data) {
    }

    @Transactional
    public UploadedImage upload(UserPrincipal uploader, MultipartFile file) {
        if (!uploader.isSeller()) {
            throw new ForbiddenException("Only sellers can upload images. Become a seller from your dashboard first - "
                    + "then you can add a logo and photos of what you sell.");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose an image to upload.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException(RejectionMessages.IMAGE_TOO_BIG);
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("We couldn't read that image. Please try another one.");
        }
        // Trust the file's actual bytes, not its name or the browser's claimed type - a renamed
        // .exe or an SVG (which can carry scripts) must never be served back as an "image".
        String contentType = sniffType(data);
        if (contentType == null) {
            throw new BadRequestException("That file isn't a supported image. Please upload a JPG, PNG or WebP photo.");
        }
        StoredImage saved = imageRepository.save(StoredImage.of(uploader.getId(), contentType, data));
        return new UploadedImage(saved.getId(), URL_PREFIX + saved.getId(), contentType, saved.getSizeBytes());
    }

    @Transactional(readOnly = true)
    public ImageContent load(UUID id) {
        StoredImage image = imageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that image. It may have been removed."));
        return new ImageContent(image.getContentType(), image.getData());
    }

    /**
     * New image URLs on listings and businesses must be photos uploaded here - not links to other
     * websites, which could change to anything after we've checked them. A value that hasn't
     * changed is always accepted, so older listings with external links can still be edited.
     */
    public static void requireUploadedImageUrl(String newUrl, String currentUrl) {
        if (newUrl == null || newUrl.isBlank() || newUrl.equals(currentUrl)) {
            return;
        }
        if (!OWN_IMAGE_URL.matcher(newUrl).matches()) {
            throw new BadRequestException("Please add photos with the upload button rather than pasting a link to another website.");
        }
    }

    static String sniffType(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (d.length >= 8 && Arrays.equals(Arrays.copyOf(d, 8),
                new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})) {
            return "image/png";
        }
        if (d.length >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}
