import { Injectable } from '@angular/core';
import SockJS from 'sockjs-client';
import * as Stomp from '@stomp/stompjs';
import { Client } from '@stomp/stompjs';
@Injectable({
  providedIn: 'root'
})
export class LogWebsocketService {
  private stompClient: Client;

  constructor() {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws')
    });
  }

  connect(callback: (message: string) => void) {
    this.stompClient.onConnect = (frame) => {
      this.stompClient.subscribe('/topic/logs', (message: Stomp.IMessage) => {
        callback(message.body);
      });
    };

    this.stompClient.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };

    this.stompClient.activate();
  }

  disconnect() {
    if (this.stompClient) {
      this.stompClient.deactivate();
    }
  }
}