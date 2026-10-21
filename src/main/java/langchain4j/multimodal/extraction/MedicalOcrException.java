package langchain4j.multimodal.extraction;

/** A user-actionable OCR service failure that is safe to return without provider response details. */
public class MedicalOcrException extends RuntimeException {

    public MedicalOcrException(String message) {
        super(message);
    }

    public MedicalOcrException(String message, Throwable cause) {
        super(message, cause);
    }
}
