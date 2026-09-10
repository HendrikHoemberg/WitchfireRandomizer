package dev.hendrikhoemberg.witchfirerandomizer.assets;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the shipped image payload.
 *
 * <p>The wiki art arrives from the wiki at full size (enemy renders are 1024x1024, ~570 KB each)
 * while the templates never draw them larger than a couple of hundred pixels. This test pins a
 * pixel and byte budget per asset group so a future scrape cannot silently re-inflate the
 * ~70 MB payload. Regenerate the assets with {@code scripts/optimize_images.py}.
 */
class ImageAssetBudgetTest {

    private static final Path IMAGES = Path.of("src/main/resources/static/images");

    private record Budget(String name, int maxDimension, long maxBytes) {
    }

    private static final List<Budget> DIRECTORY_BUDGETS = List.of(
            new Budget("enemies", 512, 130_000),
            new Budget("items", 256, 45_000),
            new Budget("arcana", 192, 28_000),
            new Budget("prophecies", 160, 25_000));

    private static final Map<String, Budget> FILE_BUDGETS = Map.of(
            "texture-transparent.webp", new Budget("texture-transparent", 256, 60_000),
            "wf-logo2.webp", new Budget("logo", 900, 220_000),
            "wf-bg.webp", new Budget("background", 3840, 180_000));

    private static String rel(Path file) {
        return IMAGES.relativize(file).toString();
    }

    private static boolean isImage(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".webp")
                || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

    private static List<Path> shippedImages() throws IOException {
        try (var walk = Files.walk(IMAGES)) {
            return walk.filter(Files::isRegularFile).filter(ImageAssetBudgetTest::isImage).sorted().toList();
        }
    }

    private static Budget budgetFor(Path file) {
        Budget byFile = FILE_BUDGETS.get(file.getFileName().toString());
        if (byFile != null) {
            return byFile;
        }
        Path parent = file.getParent();
        if (parent == null) {
            return null;
        }
        String directory = parent.getFileName().toString();
        return DIRECTORY_BUDGETS.stream().filter(b -> b.name().equals(directory)).findFirst().orElse(null);
    }

    private static int[] readDimensions(Path file) throws IOException {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".webp")) {
            return webpDimensions(file);
        }
        try (ImageInputStream in = ImageIO.createImageInputStream(file.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IOException("No ImageIO reader for " + file);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        }
    }

    /** The JDK has no WebP ImageIO reader, so read the canvas size straight out of the RIFF header. */
    private static int[] webpDimensions(Path file) throws IOException {
        byte[] header = new byte[30];
        try (var in = Files.newInputStream(file)) {
            if (in.readNBytes(header, 0, header.length) < header.length) {
                throw new IOException("Truncated WebP file: " + file);
            }
        }
        String chunk = new String(header, 12, 4, StandardCharsets.US_ASCII);
        ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        return switch (chunk) {
            case "VP8X" -> new int[]{uint24le(header, 24) + 1, uint24le(header, 27) + 1};
            case "VP8 " -> new int[]{buffer.getShort(26) & 0x3fff, buffer.getShort(28) & 0x3fff};
            default -> throw new IOException("Unsupported WebP chunk '" + chunk + "' in " + file);
        };
    }

    private static int uint24le(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8) | ((bytes[offset + 2] & 0xff) << 16);
    }

    @Test
    void shouldHaveABudgetForEveryImageWeShip() throws IOException {
        List<String> unbudgeted = new ArrayList<>();
        for (Path file : shippedImages()) {
            if (budgetFor(file) == null) {
                unbudgeted.add(rel(file));
            }
        }

        assertThat(unbudgeted)
                .as("every shipped image needs an explicit budget in this test")
                .isEmpty();
    }

    @Test
    void shouldKeepEveryImageWithinItsPixelAndByteBudget() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : shippedImages()) {
            Budget budget = budgetFor(file);
            if (budget == null) {
                continue;
            }
            int[] size = readDimensions(file);
            long bytes = Files.size(file);

            if (Math.max(size[0], size[1]) > budget.maxDimension()) {
                violations.add("%s is %dx%d px, budget is %d px"
                        .formatted(rel(file), size[0], size[1], budget.maxDimension()));
            }
            if (bytes > budget.maxBytes()) {
                violations.add("%s is %d KB, budget is %d KB"
                        .formatted(rel(file), bytes / 1024, budget.maxBytes() / 1024));
            }
        }

        assertThat(violations)
                .as("run scripts/optimize_images.py to bring these back inside budget")
                .isEmpty();
    }
}
