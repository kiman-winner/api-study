package com.payment.payment_api.service;

import com.payment.payment_api.dto.LoginRequest;
import com.payment.payment_api.dto.TokenResponse;
import com.payment.payment_api.entity.Member;
import com.payment.payment_api.repository.MemberRepository;
import com.payment.payment_api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    @Transactional
    public void signUp(LoginRequest request) {
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        }
        Member member = Member.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        memberRepository.save(member);
    }

    // 로그인
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return TokenResponse.of(jwtTokenProvider.generateToken(member.getUsername()));
    }
}
