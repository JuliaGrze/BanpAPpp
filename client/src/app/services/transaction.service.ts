import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TransactionService {
  private apiUrl = 'http://localhost:8081/api/transactions';

  constructor(private http: HttpClient) {}

  getAll(email: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/all/${email}`);
  }

  getPending(email: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/pending/${email}`);
  }

  confirm(transactionId: number): Observable<string> {
    return this.http.post(`${this.apiUrl}/confirm/${transactionId}`, {}, { responseType: 'text' });
  }

  reject(transactionId: number): Observable<string> {
    return this.http.post(`${this.apiUrl}/reject/${transactionId}`, {}, { responseType: 'text' });
  }

  addFunds(amount: number): Observable<void> {
  return this.http.post<void>(`${this.apiUrl}/add-funds`, { amount });
}

}
