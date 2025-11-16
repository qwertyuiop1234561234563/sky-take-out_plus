// AsyncService.java - 核心异步服务
package com.sky.service;

import com.sky.entity.Orders;
import com.sky.mapper.*;
import com.sky.utils.WeChatPayUtil;
import com.sky.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.websocket.Session;
import java.util.Collection;

import static com.sky.websocket.WebSocketServer.sessionMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncService {



    private final WebSocketServer webSocketServer;

    private final OrderMapper orderMapper;

    private final AddressBookMapper addressBookMapper;

    private final ShoppingCartMapper shoppingCartMapper;

    private final OrderDetailMapper orderDetailMapper;

    private final WeChatPayUtil weChatPayUtil;

    private final UserMapper userMapper;



    /**
     * 异步发送订单通知
     */
    @Async("taskExecutor")
    public void sendToAllClient(String message) {
        Collection<Session> sessions = sessionMap.values();
        for (Session session : sessions) {
            try {
                //服务器向客户端发送消息
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}