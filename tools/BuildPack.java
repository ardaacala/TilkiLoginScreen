import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.*;
import javax.imageio.ImageIO;

/** Run with Java 21: java tools/BuildPack.java path/to/original-logo.png */
class BuildPack {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Supply original logo PNG path");
        BufferedImage source = ImageIO.read(Path.of(args[0]).toFile());
        if (source == null) throw new IllegalArgumentException("Invalid image");
        Path root = Path.of("resourcepack");
        Path textures = root.resolve("assets/tilkiloginscreen/textures/font");
        Files.createDirectories(textures);
        BufferedImage logo = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        var graphics = logo.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double scale = Math.min(512d / source.getWidth(), 512d / source.getHeight());
        int width = (int)Math.round(source.getWidth() * scale);
        int height = (int)Math.round(source.getHeight() * scale);
        graphics.drawImage(source, (512-width)/2, (512-height)/2, width, height, null);
        graphics.dispose();
        ImageIO.write(logo, "png", textures.resolve("logo.png").toFile());
        BufferedImage black = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        black.setRGB(0, 0, Color.BLACK.getRGB());
        ImageIO.write(black, "png", textures.resolve("black.png").toFile());
        write(root.resolve("pack.mcmeta"), """
            {"pack":{"pack_format":46,"description":"TilkiCraft AuthMe login screen - Minecraft 1.21.4"}}
            """);
        write(root.resolve("assets/minecraft/font/default.json"), """
            {"providers":[{"type":"reference","id":"tilkiloginscreen:login"}]}
            """);
        write(root.resolve("assets/tilkiloginscreen/font/login.json"), """
            {"providers":[
              {"type":"bitmap","file":"tilkiloginscreen:font/black.png","height":4096,"ascent":2045,"chars":["\uE900"]},
              {"type":"bitmap","file":"tilkiloginscreen:font/logo.png","height":36,"ascent":15,"chars":["\uE901"]},
              {"type":"space","advances":{"\uE902":-2030,"\uE903":-2066,"\uE904":-1}}
            ]}
            """);
        Path zip = Path.of("dist/TilkiLoginScreen-ResourcePack.zip");
        Files.createDirectories(zip.getParent());
        try (var output = new ZipOutputStream(Files.newOutputStream(zip)); var files = Files.walk(root)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                ZipEntry entry = new ZipEntry(root.relativize(file).toString().replace('\\', '/'));
                entry.setTime(0);
                output.putNextEntry(entry);
                Files.copy(file, output);
                output.closeEntry();
            }
        }
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(zip)));
        Files.writeString(Path.of("dist/resourcepack-sha1.txt"), hash + "\n");
        System.out.println("Pack created. SHA1: " + hash);
    }
    static void write(Path path, String text) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, text);
    }
}
