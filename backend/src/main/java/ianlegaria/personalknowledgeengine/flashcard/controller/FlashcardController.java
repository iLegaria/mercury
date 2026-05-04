package ianlegaria.personalknowledgeengine.flashcard.controller;

import ianlegaria.personalknowledgeengine.flashcard.dto.*;
import ianlegaria.personalknowledgeengine.flashcard.service.FlashcardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Flashcards", description = "Spaced repetition flashcard system using the SM-2 algorithm")
@RestController
@RequestMapping("/api/v1/flashcards")
@RequiredArgsConstructor
public class FlashcardController {

    private final FlashcardService flashcardService;

    @Operation(summary = "Create flashcard deck")
    @ApiResponse(responseCode = "201", description = "Deck created")
    @PostMapping("/decks")
    public ResponseEntity<DeckResponse> createDeck(@Valid @RequestBody CreateDeckRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flashcardService.createDeck(req));
    }

    @Operation(summary = "List user decks", description = "Returns flashcard decks for a user. Supports pagination via page/size/sort query params.")
    @GetMapping("/decks/user/{userId}")
    public ResponseEntity<Page<DeckResponse>> getDecksByUser(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(flashcardService.getDecksByUser(userId, pageable));
    }

    @Operation(summary = "Delete deck")
    @ApiResponse(responseCode = "204", description = "Deck deleted")
    @ApiResponse(responseCode = "404", description = "Deck not found")
    @DeleteMapping("/decks/{deckId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeck(@PathVariable UUID deckId, @RequestParam UUID userId) {
        flashcardService.deleteDeck(deckId, userId);
    }

    @Operation(summary = "Import cards from CSV",
            description = """
                    Imports flashcards from a CSV file into the specified deck.
                    Expected format: two columns — front,back — one card per row.
                    Empty rows are skipped. Returns a summary with imported and skipped counts.
                    """)
    @PostMapping(value = "/decks/{deckId}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importCards(
            @PathVariable UUID deckId,
            @RequestParam UUID userId,
            @RequestParam("file") MultipartFile file) {
        return flashcardService.importCards(deckId, userId, file);
    }

    @Operation(summary = "List deck cards")
    @GetMapping("/decks/{deckId}/cards")
    public List<CardResponse> getCards(@PathVariable UUID deckId, @RequestParam UUID userId) {
        return flashcardService.getCards(deckId, userId);
    }

    @Operation(summary = "Get cards due for review",
            description = "Returns cards scheduled for review today or earlier, based on the SM-2 spaced repetition algorithm.")
    @GetMapping("/decks/{deckId}/cards/due")
    public List<CardResponse> getDueCards(@PathVariable UUID deckId, @RequestParam UUID userId) {
        return flashcardService.getDueCards(deckId, userId);
    }

    @Operation(summary = "Toggle WhatsApp reminder for a deck")
    @ApiResponse(responseCode = "204", description = "Setting updated")
    @PatchMapping("/decks/{deckId}/whatsapp-reminder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setWhatsappReminder(
            @PathVariable UUID deckId,
            @Valid @RequestBody SetWhatsappReminderRequest req) {
        flashcardService.setWhatsappReminder(deckId, req.userId(), req.enabled());
    }

    @Operation(summary = "Add a card to a deck")
    @ApiResponse(responseCode = "201", description = "Card created")
    @PostMapping("/cards")
    public ResponseEntity<CardResponse> addCard(@Valid @RequestBody CreateCardRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flashcardService.addCard(req));
    }

    @Operation(summary = "Update card question and answer")
    @ApiResponse(responseCode = "200", description = "Card updated")
    @PatchMapping("/cards/{cardId}")
    public CardResponse updateCard(@PathVariable UUID cardId, @Valid @RequestBody UpdateCardRequest req) {
        return flashcardService.updateCard(cardId, req.userId(), req.question(), req.answer());
    }

    @Operation(summary = "Delete a card")
    @ApiResponse(responseCode = "204", description = "Card deleted")
    @DeleteMapping("/cards/{cardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable UUID cardId, @RequestParam UUID userId) {
        flashcardService.deleteCard(cardId, userId);
    }

    @Operation(summary = "Review a card",
            description = """
                    Submits a quality rating (0–5) for a card review:
                    - 0–2: complete or near blackout — interval resets to 1 day.
                    - 3: correct with difficulty — interval grows slowly.
                    - 4–5: correct and easy — interval grows rapidly.
                    The SM-2 algorithm recalculates the ease factor and next review date.
                    """)
    @ApiResponse(responseCode = "200", description = "Review recorded, next review date returned")
    @ApiResponse(responseCode = "404", description = "Card not found")
    @PostMapping("/cards/{cardId}/review")
    public ReviewResult reviewCard(@PathVariable UUID cardId, @Valid @RequestBody ReviewCardRequest req) {
        return flashcardService.reviewCard(cardId, req.userId(), req.quality());
    }
}
