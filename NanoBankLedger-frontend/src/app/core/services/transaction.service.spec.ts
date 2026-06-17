import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TransactionService } from './transaction.service';
import { Transaction } from '../models/transaction.models';

describe('TransactionService', () => {
  let service: TransactionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TransactionService]
    });
    service = TestBed.inject(TransactionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should GET transactions by wallet with filters', () => {
    const mockResponse: Transaction[] = [];

    service.findByWalletId('1', { type: 'INCOME' }).subscribe(r => {
      expect(r).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(r =>
      r.url.includes('/api/v1/wallets/1/transactions') && r.params.get('type') === 'INCOME'
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.has('page')).toBeFalse();
    expect(req.request.params.has('size')).toBeFalse();
    req.flush(mockResponse);
  });

  it('should GET transactions by wallet without filters', () => {
    const mockResponse: Transaction[] = [];

    service.findByWalletId('1').subscribe(r => {
      expect(r).toEqual(mockResponse);
    });

    const req = httpMock.expectOne('/api/v1/wallets/1/transactions');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys().length).toBe(0);
    req.flush(mockResponse);
  });

  it('should GET transaction by id', () => {
    const mockTransaction = {
      id: '1', wallet_id: '1', type: 'EXPENSE' as const, amount: '50.00',
      date: new Date().toISOString(), created_at: '', updated_at: ''
    };

    service.findById('1').subscribe(r => expect(r).toEqual(mockTransaction));

    const req = httpMock.expectOne('/api/v1/transactions/1');
    expect(req.request.method).toBe('GET');
    req.flush(mockTransaction);
  });

  it('should POST to create transaction', () => {
    const mockTransaction = {
      id: '1', wallet_id: '1', type: 'EXPENSE' as const, amount: '50.00',
      date: new Date().toISOString(), created_at: '', updated_at: ''
    };
    const createRequest = { type: 'EXPENSE' as const, amount: '50.00', category_id: 'cat1' };

    service.create('1', createRequest).subscribe(r => expect(r).toEqual(mockTransaction));

    const req = httpMock.expectOne('/api/v1/wallets/1/transactions');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(createRequest);
    req.flush(mockTransaction);
  });

  it('should PATCH to update transaction', () => {
    const mockTransaction = {
      id: '1', wallet_id: '1', type: 'INCOME' as const, amount: '100.00',
      date: new Date().toISOString(), created_at: '', updated_at: ''
    };
    const updateRequest = { amount: '100.00', type: 'INCOME' as const };

    service.update('1', updateRequest).subscribe(r => expect(r).toEqual(mockTransaction));

    const req = httpMock.expectOne('/api/v1/transactions/1');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual(updateRequest);
    req.flush(mockTransaction);
  });

  it('should DELETE transaction', () => {
    service.delete('1').subscribe();

    const req = httpMock.expectOne('/api/v1/transactions/1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should PATCH move transaction to another wallet', () => {
    const mockTransaction = {
      id: '1', wallet_id: '2', type: 'EXPENSE' as const, amount: '50.00',
      date: new Date().toISOString(), created_at: '', updated_at: ''
    };

    service.moveToWallet('1', { target_wallet_id: '2' }).subscribe(r => expect(r).toEqual(mockTransaction));

    const req = httpMock.expectOne('/api/v1/transactions/1/move');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ target_wallet_id: '2' });
    req.flush(mockTransaction);
  });

  it('should handle error on findByWalletId', () => {
    service.findByWalletId('1').subscribe({
      error: (err) => {
        expect(err.status).toBe(404);
      }
    });

    const req = httpMock.expectOne('/api/v1/wallets/1/transactions');
    req.flush('Not found', { status: 404, statusText: 'Not Found' });
  });
});
