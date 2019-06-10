/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tim.saga.demo.springcloud.order.service.impl;

import com.tim.saga.core.annotation.SagaParticipative;
import com.tim.saga.core.annotation.SagaTransactional;
import com.tim.saga.demo.springcloud.order.client.AccountClient;
import com.tim.saga.demo.springcloud.order.client.InventoryClient;
import com.tim.saga.demo.springcloud.order.dto.AccountDTO;
import com.tim.saga.demo.springcloud.order.dto.InventoryDTO;
import com.tim.saga.demo.springcloud.order.entity.Order;
import com.tim.saga.demo.springcloud.order.enums.OrderStatusEnum;
import com.tim.saga.demo.springcloud.order.mapper.OrderMapper;
import com.tim.saga.demo.springcloud.order.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * PaymentServiceImpl.
 *
 * @author xiaobing
 */
@Service
@SuppressWarnings("all")
public class PaymentServiceImpl implements PaymentService {

	/**
	 * logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentServiceImpl.class);

	private final OrderMapper orderMapper;

	private final AccountClient accountClient;

	private final InventoryClient inventoryClient;

	@Autowired(required = false)
	public PaymentServiceImpl(OrderMapper orderMapper,
	                          AccountClient accountClient,
	                          InventoryClient inventoryClient) {
		this.orderMapper = orderMapper;
		this.accountClient = accountClient;
		this.inventoryClient = inventoryClient;
	}

	@Override
	@SagaTransactional
	public void makePayment(Order order) {
		this.successPayOrder(order);

		AccountDTO accountDTO = new AccountDTO();
		accountDTO.setAmount(order.getTotalAmount());
		accountDTO.setUserId(order.getUserId());
		accountDTO.setOrderId(order.getId());
		LOGGER.debug("===========执行springcloud扣减资金接口==========");
		accountClient.payment(accountDTO);

		//进入扣减库存操作
		InventoryDTO inventoryDTO = new InventoryDTO();
		inventoryDTO.setCount(order.getCount());
		inventoryDTO.setProductId(order.getProductId());
		inventoryDTO.setOrderId(order.getId());
		inventoryClient.decrease(inventoryDTO);
	}

	@Override
	@SagaTransactional
	public void mockPaymentInventoryWithException(Order order) {
		LOGGER.debug("===========执行springcloud  mockPaymentInventoryWithException 扣减资金接口==========");
		this.successPayOrder(order);

		AccountDTO accountDTO = new AccountDTO();
		accountDTO.setAmount(order.getTotalAmount());
		accountDTO.setUserId(order.getUserId());
		accountDTO.setOrderId(order.getId());


		accountClient.payment(accountDTO);

		//进入扣减库存操作
		InventoryDTO inventoryDTO = new InventoryDTO();
		inventoryDTO.setCount(order.getCount());
		inventoryDTO.setProductId(order.getProductId());
		inventoryDTO.setOrderId(order.getId());
		inventoryClient.decreaseWithException(inventoryDTO);
	}

	@Override
	@SagaTransactional
	public void mockPaymentInventoryWithShutdown(Order order) {
		LOGGER.debug("===========执行springcloud  mockPaymentInventoryWithShutdown 扣减资金接口==========");
		this.successPayOrder(order);

		AccountDTO accountDTO = new AccountDTO();
		accountDTO.setAmount(order.getTotalAmount());
		accountDTO.setUserId(order.getUserId());
		accountDTO.setOrderId(order.getId());

		accountClient.payment(accountDTO);

		//扣库存阶段系统宕机
		System.exit(1);
	}

	public void successPayOrder(Order order) {
		order.setStatus(OrderStatusEnum.PAY_SUCCESS.getCode());
		orderMapper.update(order);
		LOGGER.info("=========进行订单cancel操作完成================");
	}

	public void cancelPayment(Order order) {
		order.setStatus(OrderStatusEnum.PAY_FAIL.getCode());
		orderMapper.update(order);
		LOGGER.info("=========进行订单cancel操作完成================");
	}

}
