package com.example.monitoringservice.mysql.repositories;

import com.example.monitoringservice.mysql.tables.CommunicationTable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommunicationTableRepository extends CrudRepository<CommunicationTable, Integer> {
    @Query("SELECT connection FROM CommunicationTable connection " +
            "WHERE (connection.service1 = :service1 AND connection.service2 = :service2) OR (connection.service1 = :service2 AND connection.service2 = :service1)")
    CommunicationTable findFirstConnectionBetweenServices(@Param("service1") String service1,
                                                          @Param("service2") String service2);

    List<CommunicationTable> findAll();
}
