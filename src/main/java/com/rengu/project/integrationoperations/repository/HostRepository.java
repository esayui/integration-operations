package com.rengu.project.integrationoperations.repository;

import com.rengu.project.integrationoperations.entity.AllHost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author yaojiahao
 * @data 2019/4/29 19:17
 */
@Repository
public interface HostRepository extends JpaRepository<AllHost,String> {

}
