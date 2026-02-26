package com.example.boardservice.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.boardservice.client.PointClient;
import com.example.boardservice.client.UserClient;
import com.example.boardservice.domain.Board;
import com.example.boardservice.domain.BoardRepositroy;
import com.example.boardservice.domain.User;
import com.example.boardservice.dto.BoardResponseDto;
import com.example.boardservice.dto.CreateBoardRequestDto;
import com.example.boardservice.dto.UserDto;
import com.example.boardservice.event.BoardCreatedEvent;

import jakarta.transaction.Transactional;

@Service
public class BoardService {

    private final BoardRepositroy boardRepositroy;
    private final PointClient pointClient;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public BoardService(
        BoardRepositroy boardRepositroy, 
        UserClient userClient, 
        PointClient pointClient,
        KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.boardRepositroy = boardRepositroy;
        this.pointClient = pointClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public void create(CreateBoardRequestDto createBoardRequestDto, Long userId) {
        boolean isPointDeducted = false;
        boolean isBoardCreated = false;
        Long boardId = null;

        try {
            pointClient.deductPoint(userId, 100);
            isPointDeducted = true;
            System.out.println("포인트 차감 성공");

            Board board = new Board(
                createBoardRequestDto.getTitle(),
                createBoardRequestDto.getContent(), 
                userId
            );
        
            Board savedBoard = boardRepositroy.save(board);
            boardId = savedBoard.getBoardId();
            isBoardCreated = true;
            System.out.println("게시글 저장 성공");

            // userClient.addActivityScore(userId, 10);
            // System.out.println("포인트 적립 성공");

            BoardCreatedEvent boardCreatedEvent = new BoardCreatedEvent(userId);
            kafkaTemplate.send("board.created", boardCreatedEvent.toJsonString());
        } catch (Exception e) {
            if (isBoardCreated) {
                this.boardRepositroy.deleteById(boardId);
                System.out.println("[보상 트랜잭션] 게시글 삭제");
            }

            if (isPointDeducted) {
                pointClient.addPoint(userId, 100);
                System.out.println("[보상 트랜잭션] 포인트 적립");
            }

            throw e;
        }
    }

    // public BoardResponseDto getBoard(Long boardId) {
    //     Board board = boardRepositroy.findById(boardId)
    //         .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

    //     Optional<UserResponseDto> optionalUserResponseDto = userClient.fetchUser(board.getUserId());

    //     UserDto userDto = null;
    //     if (optionalUserResponseDto.isPresent()) {
    //         UserResponseDto userResponseDto = optionalUserResponseDto.get();
    //         userDto = new UserDto(
    //             userResponseDto.getUserId(), 
    //             userResponseDto.getName()
    //         );
    //     }

    //     BoardResponseDto boardResponseDto = new BoardResponseDto(boardId, board.getTitle(), board.getContent(), userDto);

    //     return boardResponseDto;
    // }

    // public List<BoardResponseDto> getBoards() {
    //     List<Board> boards = boardRepositroy.findAll();

    //     List<Long> ids = boards.stream()
    //         .map(Board::getUserId)
    //         .distinct()
    //         .toList();

    //     List<UserResponseDto> userResponseDtos = userClient.fetchUsersByIds(ids);

    //     Map<Long, UserDto> userMap = new HashMap<>();

    //     userResponseDtos.forEach(dto -> {
    //         Long userId = dto.getUserId();
    //         userMap.put(userId, new UserDto(userId, dto.getName()));
    //     });

    //     return boards.stream()
    //         .map(board -> new BoardResponseDto(
    //             board.getBoardId(),
    //             board.getTitle(),
    //             board.getContent(),
    //             userMap.get(board.getUserId())
    //         ))
    //         .toList();
    // }

    public BoardResponseDto getBoard(Long boardId) {
        Board board = this.boardRepositroy.findById(boardId).orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        User user = board.getUser();

        return new BoardResponseDto(
            board.getBoardId(),
            board.getTitle(),
            board.getContent(),
            new UserDto(
                user.getUserId(),
                user.getName()   
            )
        );
    }

    @Cacheable(cacheNames = "getBoards", key = "'boards:page:' + #page + ':size:' + #size", cacheManager = "boardCacheManager")
    public List<BoardResponseDto> getBoards(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "boardId"));

        Page<Board> boardPage = this.boardRepositroy.findAll(pageable);

        return boardPage.getContent().stream()
            .map(board -> new BoardResponseDto(
                board.getBoardId(),
                board.getTitle(),
                board.getContent(),
                new UserDto(
                    board.getUser().getUserId(),
                    board.getUser().getName()   
                )
            ))
            .toList();
    }
}
