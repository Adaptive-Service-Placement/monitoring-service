package com.example.monitoringmanager.mysql.repositories;

import com.example.monitoringmanager.mysql.tables.CommunicationTable;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CommunicationTableRepository extends CrudRepository<CommunicationTable, Integer> {
    CommunicationTable findFirstByService1AndService2(String service1, String service2);

    List<CommunicationTable> findAll();
}
