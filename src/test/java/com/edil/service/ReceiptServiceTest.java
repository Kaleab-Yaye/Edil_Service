package com.edil.service;

import com.edil.dto.request.HasReceiptBeenUsedBeforeRequest;
import com.edil.dto.response.HasReceiptBeenUsedBeforeResponse;
import com.edil.dto.response.UploadReceiptResponse;
import com.edil.repository.ReceiptRepository;
import com.edil.util.QrCodeUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private QrCodeUtil qrCodeUtil;

    private Cache<UUID, Boolean> uploadKeyCache;
    private ReceiptService receiptService;

    @BeforeEach
    void setUp() {
        uploadKeyCache = Caffeine.newBuilder().build();
        receiptService = new ReceiptService(receiptRepository, uploadKeyCache, qrCodeUtil);
    }

    @Nested
    @DisplayName("checkForReceiptExitance Tests")
    class CheckReceiptExistenceTests {

        @Test
        @DisplayName("Should return 406 NOT_ACCEPTABLE when URL does not match CBE format")
        void shouldRejectInvalidCbeUrl() {
            HasReceiptBeenUsedBeforeRequest request = new HasReceiptBeenUsedBeforeRequest("https://invalid-cbe.com/abc");

            ResponseEntity<HasReceiptBeenUsedBeforeResponse> response = receiptService.checkForReceiptExitance(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isValidUrl()).isFalse();
            assertThat(response.getBody().hasBeenUsed()).isNull();
        }

        @Test
        @DisplayName("Should return 200 OK with hasBeenUsed=true when receipt exists in database")
        void shouldReturnHasBeenUsedTrueWhenReceiptExists() {
            String url = "https://mbreciept.cbe.com.et/v2-TXN998877";
            when(receiptRepository.existsReceiptById("TXN998877")).thenReturn(true);

            ResponseEntity<HasReceiptBeenUsedBeforeResponse> response =
                    receiptService.checkForReceiptExitance(new HasReceiptBeenUsedBeforeRequest(url));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isValidUrl()).isTrue();
            assertThat(response.getBody().hasBeenUsed()).isTrue();
        }

        @Test
        @DisplayName("Should return 200 OK with hasBeenUsed=false when receipt is new and unused")
        void shouldReturnHasBeenUsedFalseWhenReceiptNew() {
            String url = "https://mbreciept.cbe.com.et/v2-TXN112233";
            when(receiptRepository.existsReceiptById("TXN112233")).thenReturn(false);

            ResponseEntity<HasReceiptBeenUsedBeforeResponse> response =
                    receiptService.checkForReceiptExitance(new HasReceiptBeenUsedBeforeRequest(url));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isValidUrl()).isTrue();
            assertThat(response.getBody().hasBeenUsed()).isFalse();
        }

        @Test
        @DisplayName("String overloaded method should properly identify used receipts")
        void shouldWorkWithStringOverload() {
            String url = "https://mbreciept.cbe.com.et/v2-OVERLOADED123";
            when(receiptRepository.existsReceiptById("OVERLOADED123")).thenReturn(true);

            HasReceiptBeenUsedBeforeResponse result = receiptService.checkForReceiptExitance(url);

            assertThat(result.isValidUrl()).isTrue();
            assertThat(result.hasBeenUsed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Upload Key Cache Tests")
    class UploadKeyTests {

        @Test
        @DisplayName("createUploadReceiptKey should generate UUID, store in cache and return 200 OK")
        void shouldGenerateAndCacheUploadReceiptKey() {
            ResponseEntity<UploadReceiptResponse> response = receiptService.createUploadReceiptKey();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            UUID key = response.getBody().uploadKey();
            assertThat(key).isNotNull();
            assertThat(uploadKeyCache.getIfPresent(key)).isTrue();
        }

        @Test
        @DisplayName("checkForUploadReceiptKey should return 200 OK for valid cached key with proper URI structure")
        void shouldAcceptValidUploadKeyUri() {
            UUID validKey = UUID.randomUUID();
            uploadKeyCache.put(validKey, true);

            // Structure: "" / "upload" / "receipt" / "{key}" -> length 4
            String uri = "/upload/receipt/" + validKey;

            ResponseEntity<Void> response = receiptService.checkForUploadReceiptKey(uri);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("checkForUploadReceiptKey should return 403 FORBIDDEN for malformed URI path")
        void shouldRejectMalformedUri() {
            ResponseEntity<Void> response = receiptService.checkForUploadReceiptKey("/invalid/path");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("checkForUploadReceiptKey should return 403 FORBIDDEN for non-UUID key segment")
        void shouldRejectNonUuidKey() {
            ResponseEntity<Void> response = receiptService.checkForUploadReceiptKey("/upload/receipt/not-a-uuid");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("checkForUploadReceiptKey should return 403 FORBIDDEN when key is not in cache")
        void shouldRejectUncachedKey() {
            UUID unknownKey = UUID.randomUUID();
            String uri = "/upload/receipt/" + unknownKey;

            ResponseEntity<Void> response = receiptService.checkForUploadReceiptKey(uri);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
