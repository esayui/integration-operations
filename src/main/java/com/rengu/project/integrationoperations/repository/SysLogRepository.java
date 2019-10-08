package com.rengu.project.integrationoperations.repository;

import com.rengu.project.integrationoperations.entity.SysLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Repository
public interface SysLogRepository extends JpaRepository<SysLogEntity,String> {
    Page<SysLogEntity> findAll(Pageable pageable);

    void deleteByCreateTimeBetween(Date st, Date ed);
}
