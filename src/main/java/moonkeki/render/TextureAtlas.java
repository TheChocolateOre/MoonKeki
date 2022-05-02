package moonkeki.render;

import moonkeki.util.rpacking.Position;
import moonkeki.util.rpacking.PackingFailedException;
import moonkeki.util.rpacking.PackingSolution;
import moonkeki.util.rpacking.RectanglePacker;
import moonkeki.util.rpacking.Size;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface TextureAtlas<K> extends AutoCloseable {

    sealed class Builder<K> {
        private static final class EnumKeyBuilder<K extends Enum<K>> extends
                Builder<K> {
            private final Class<K> CLS;

            private EnumKeyBuilder(Class<K> cls) {
                this.CLS = cls;
            }

            @Override
            Map<K, Pixmap> createMap(int capacity) {
                return new EnumMap<>(this.CLS);
            }
        }

        private Stream<Map.Entry<K, Texture>> textures;
        private Size maxSize;
        private RectanglePacker rectanglePacker = RectanglePacker.getDefault();

        private Builder() {}

        public Builder<K> ofTextures(Stream<Map.Entry<K, Texture>> textures) {
            this.textures = textures;
            return this;
        }

        public Builder<K> ofMaxSize(Size maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder<K> ofRectanglePacker(RectanglePacker rectanglePacker) {
            this.rectanglePacker = rectanglePacker;
            return this;
        }

        public TextureAtlas<K> build() throws PackingFailedException {
            //Objectless state
            //TODO Maybe should be a (K, Texture) tuple record
            final PackingSolution<Map.Entry<K, Texture>> SOLUTION =
                    this.rectanglePacker.pack(this.textures,
                            e -> e.getValue().getWidth(),
                            e -> e.getValue().getHeight(),
                            this.maxSize);
            final Map<K, Pixmap> PIXMAP_CACHE = this.createMap(Math.toIntExact(
                    SOLUTION.itemCount()));
            final Texture TEXTURE = new Texture(SOLUTION.getWidth(),
                    SOLUTION.getHeight(), new Color(0.0f, 0.0f, 0.0f, 0.0f));

            try (final InstantRenderer RENDERER =
                       InstantRenderer.builder()
                                      .ofCanvas(TEXTURE)
                                      .shortLived()
                                      .build()) {
                SOLUTION.itemStream().forEach(i -> {
                    Texture t = i.getKey().getValue();
                    Position pos = i.getValue();
                    RENDERER.drawCommand()
                            .ofPixmap(t)
                            .atPosition(pos.x(), pos.y())
                            .draw();
                    PIXMAP_CACHE.put(i.getKey().getKey(), TEXTURE.subRegion(
                            pos.x(), pos.y(), t.getWidth(), t.getHeight()));
                });
            }

            return new TextureAtlas<>() {
                boolean closed;

                @Override
                public Optional<Pixmap> get(K key) {
                    this.ensureOpen();
                    return Optional.ofNullable(PIXMAP_CACHE.get(key));
                }

                @Override
                public Stream<Map.Entry<K, Pixmap>> stream() {
                    return PIXMAP_CACHE.entrySet().stream();
                }

                @Override
                public boolean isClosed() {
                    return this.closed;
                }

                @Override
                public void close() {
                    if (this.isClosed()) {
                        return;
                    }

                    TEXTURE.close();
                    this.closed = true;
                }

                private void ensureOpen() throws IllegalStateException {
                    if (this.isClosed()) {
                        throw new IllegalStateException("This TextureAtlas " +
                                "is closed.");
                    }
                }
            };
        }

        Map<K, Pixmap> createMap(int capacity) {
            return new HashMap<>(capacity);
        }
    }

    static <K> Builder<K> builder() {
        return new Builder<>();
    }

    static <K extends Enum<K>> Builder<K> builder(Class<K> cls) {
        return new Builder.EnumKeyBuilder<>(cls);
    }

    //title of texture -> key
    static <K> TextureAtlas<K> load(Path path,
                                    Function<String, K> keyExtractor) throws
                                    IOException, SAXException {
        return TextureAtlas.load(path, keyExtractor, HashMap::new);
    }

    static <K extends Enum<K>> TextureAtlas<K> load(Path path,
            Function<String, K> keyExtractor, Class<K> cls) throws IOException,
            SAXException {
        return TextureAtlas.load(path, keyExtractor, () -> new EnumMap<>(cls));
    }

    static <K> void save(TextureAtlas<K> textureAtlas, Path path,
                         Function<K, String> keyNameExtractor,
                         Function<Texture, String> textureNameExtractor) {
        DocumentBuilder builder = null;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        final Texture TEXTURE = textureAtlas.stream()
                                            .findAny()
                                            .map(Map.Entry::getValue)
                                            .map(Pixmap::getTexture)
                                            .orElseThrow();
        Document xmlDocument = Objects.requireNonNull(builder).newDocument();

        //Root
        final Element ROOT = xmlDocument.createElement("texture_atlas");
        xmlDocument.appendChild(ROOT);

        //texture_name
        Element textureNameNode = xmlDocument.createElement(
                "texture_name");
        ROOT.appendChild(textureNameNode);
        textureNameNode.appendChild(xmlDocument.createTextNode(
                textureNameExtractor.apply(TEXTURE)));

        //regions
        Element regionsNode = xmlDocument.createElement("regions");
        ROOT.appendChild(regionsNode);
        Consumer<Map.Entry<K, Pixmap>> addRegion = e -> {
            Element regionNode = xmlDocument.createElement("region");
            regionsNode.appendChild(regionNode);

            //(nodeName, value)
            BiConsumer<String, String> add = (n, v) -> {
                Element node = xmlDocument.createElement(n);
                regionNode.appendChild(node);
                node.appendChild(xmlDocument.createTextNode(v));
            };

            add.accept("name", keyNameExtractor.apply(e.getKey()));
            add.accept("x", String.valueOf(e.getValue().getXOffset()));
            add.accept("y", String.valueOf(e.getValue().getYOffset()));
            add.accept("width", String.valueOf(e.getValue().getWidth()));
            add.accept("height", String.valueOf(e.getValue().getHeight()));
        };
        textureAtlas.stream().forEach(addRegion);

        try {
            Transformer transformer = TransformerFactory.newInstance()
                    .newTransformer();
            Result output = new StreamResult(path.toFile());
            Source input = new DOMSource(xmlDocument);
            transformer.transform(input, output);
        } catch (TransformerException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    private static <K> TextureAtlas<K> load(Path path,
            Function<String, K> keyExtractor,
            Supplier<Map<K, Pixmap>> mapSupplier) throws IOException,
            SAXException {
        DocumentBuilder builder = null;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        Document xmlDocument = Objects.requireNonNull(builder)
                .parse(path.toFile());
        record Node(Element element) {
            String getName() {
                return this.element.getTagName();
            }

            String getContent() {
                return this.element.getTextContent();
            }

            Optional<Node> selectChild(String name) {
                return this.childStream()
                           .filter(n -> n.getName().equals(name))
                           .findFirst();
            }

            Stream<Node> childStream() {
                NodeList l = this.element.getChildNodes();
                return IntStream.range(0, l.getLength())
                                .mapToObj(l::item)
                                .filter(n -> n.getNodeType() ==
                                             Element.ELEMENT_NODE)
                                .map(n -> (Element) n)
                                .map(Node::new);
            }

            Map<String, String> toMap() {
                return this.childStream()
                           .collect(Collectors.toUnmodifiableMap(Node::getName,
                                   Node::getContent));
            }
        }

        final Texture TEXTURE = new Node(xmlDocument.getDocumentElement())
                .selectChild("texture_name")
                .map(n -> {
                    try {
                        return new Texture(n.getContent());
                    } catch (IOException e) {
                        throw new RuntimeException();
                    }
                })
                .orElseThrow();
        final Map<K, Pixmap> REGIONS = Collections.unmodifiableMap(
                new Node(xmlDocument.getDocumentElement())
                .selectChild("regions")
                .orElseThrow()
                .childStream()
                .map(Node::toMap)
                .collect(Collectors.toMap(
                        m -> keyExtractor.apply(m.get("name")),
                        m -> {
                            final int X = Integer.parseInt(m.get("x"));
                            final int Y = Integer.parseInt(m.get("y"));
                            final int WIDTH = Integer.parseInt(m.get("width"));
                            final int HEIGHT =
                                    Integer.parseInt(m.get("height"));
                            return TEXTURE.subRegion(X, Y, WIDTH, HEIGHT);
                        }, (v1, v2) -> v1, mapSupplier)));

        return new TextureAtlas<K>() {
            boolean closed;

            @Override
            public Optional<Pixmap> get(K key) {
                this.ensureOpen();
                return Optional.ofNullable(REGIONS.get(key));
            }

            @Override
            public Stream<Map.Entry<K, Pixmap>> stream() {
                return REGIONS.entrySet().stream();
            }

            @Override
            public boolean isClosed() {
                return this.closed;
            }

            @Override
            public void close() {
                if (this.isClosed()) {
                    return;
                }

                TEXTURE.close();
                this.closed = true;
            }

            void ensureOpen() throws IllegalStateException {
                if (this.isClosed()) {
                    throw new IllegalStateException("This TextureAtlas is " +
                            "closed.");
                }
            }
        };
    }

    Optional<Pixmap> get(K key);
    Stream<Map.Entry<K, Pixmap>> stream();
    boolean isClosed();

}
