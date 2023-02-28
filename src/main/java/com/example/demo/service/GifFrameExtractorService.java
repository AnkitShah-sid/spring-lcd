package com.example.demo.service;

import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@Service
@NoArgsConstructor
public class GifFrameExtractorService {
    private static final Logger logger = LoggerFactory.getLogger(GifFrameExtractorService.class);
    private final ArrayList<BufferedImage> frames = new ArrayList<>();
    private final ArrayList<Integer> delays = new ArrayList<>();

    public void extractGifFrames(String filePath, GifFrameExtractorCallback callback) {
        try {
            clearFrames();
            File gifFile = new File(filePath);
            ImageInputStream inputStream = ImageIO.createImageInputStream(gifFile);
            ImageReader reader = ImageIO.getImageReaders(inputStream).next();
            reader.setInput(inputStream);
            int numFrames = reader.getNumImages(true);
            for (int i = 0; i < numFrames; i++) {
                BufferedImage frame = reader.read(i);
                int delay = getDelay(reader, i) + 100;
                frames.add(frame);
                delays.add(delay);
                callback.onFrameExtracted(frame, delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    logger.error("extractGifFrames 1 : " + e);
                    // If the thread is interrupted, stop the extraction
                    return;
                }
            }
            reader.dispose();
            inputStream.close();
            int index = 0;
            while (!Thread.currentThread().isInterrupted()) {
                logger.info("GIF Replayed");
                BufferedImage frame = frames.get(index);
                int delay = delays.get(index);
                callback.onFrameExtracted(frame, delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    logger.error("extractGifFrames 2 : " + e);
                    Thread.currentThread().interrupt();
                    break;
                }
                index++;
                if (index >= frames.size()) {
                    index = 0;
                }
                logger.info(String.valueOf(Thread.currentThread().isInterrupted()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String extractGifFrames2(String filePath, GifFrameExtractorCallback callback, Long duration) {
        duration = duration * 1000;
        ArrayList<BufferedImage> frames = new ArrayList<>();
        ArrayList<Integer> delays = new ArrayList<>();
        long totalDelay = 0;
        try {
            clearFrames();
            File gifFile = new File(filePath);
            ImageInputStream inputStream = ImageIO.createImageInputStream(gifFile);
            ImageReader reader = ImageIO.getImageReaders(inputStream).next();
            reader.setInput(inputStream);
            int numFrames = reader.getNumImages(true);
            for (int i = 0; i < numFrames; i++) {
                if (duration.compareTo(totalDelay) < 0) {
                    return "Finished";
                }
                BufferedImage frame = reader.read(i);
                int delay = getDelay(reader, i) + 100;
                frames.add(frame);
                delays.add(delay);
                totalDelay += delay;
                callback.onFrameExtracted(frame, delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    logger.error("extractGifFrames 1 : " + e);// If the thread is interrupted, stop the extraction
                    return "finished : " + e;
                }
            }
            reader.dispose();
            inputStream.close();
            int index = 0;
            while (!Thread.currentThread().isInterrupted()) {
                if (duration.compareTo(totalDelay) < 0) {
                    return "Finished";
                }
                BufferedImage frame = frames.get(index);
                int delay = delays.get(index);
                callback.onFrameExtracted(frame, delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    logger.error("extractGifFrames 1 : " + e);// If the thread is interrupted, stop the extraction
                    return "finished : " + e;
                }
                index++;
                if (index >= frames.size()) {
                    index = 0;
                }
                logger.info(String.valueOf(Thread.currentThread().isInterrupted()));
                totalDelay += delay;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "finished : " + e;
        }
        return "finished : No Errors";
    }

    private int getDelay(ImageReader reader, int imageIndex) throws IOException {
        int delay = 0;
        IIOMetadata metadata = reader.getImageMetadata(imageIndex);
        String[] metadataNames = metadata.getMetadataFormatNames();
        for (String metadataName : metadataNames) {
            Node root = metadata.getAsTree(metadataName);
            NodeList childNodes = root.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if ("GraphicControlExtension".equals(node.getNodeName())) {
                    NamedNodeMap attributes = node.getAttributes();
                    Node delayNode = attributes.getNamedItem("delayTime");
                    if (delayNode != null) {
                        delay = Integer.parseInt(delayNode.getNodeValue()) * 10;
                        break;
                    }
                }
            }
        }
        return delay;
    }

    public void clearFrames() {
        logger.warn("clearFrames()");
        frames.clear();
        delays.clear();
    }

    public interface GifFrameExtractorCallback {
        void onFrameExtracted(BufferedImage frame, int frameDelay);
    }
}
