package com.example.monitoringservice.mysql.repositories;

import com.example.monitoringservice.mysql.tables.ServiceTable;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ServiceTableRepository extends CrudRepository<ServiceTable, Integer> {
    ServiceTable findFirstByQueueName(String queueName);

    ServiceTable findFirstByServiceIp(String serviceIp);

    List<ServiceTable> findAll();
}
