package com.himedia.sp_server.repository;

import com.himedia.sp_server.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, String> {

    Member findByUserid(String username);
}
