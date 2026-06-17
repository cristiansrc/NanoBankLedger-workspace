import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { WalletService } from './wallet.service';

describe('WalletService', () => {
  let service: WalletService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [WalletService]
    });
    service = TestBed.inject(WalletService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should GET all wallets', () => {
    const mockWallets = [
      { id: '1', name: 'Test', type: 'SAVINGS' as const, balance: '100.00', user_id: '1', created_at: '', updated_at: '' }
    ];

    service.findAll().subscribe(wallets => {
      expect(wallets).toEqual(mockWallets);
    });

    const req = httpMock.expectOne('/api/v1/wallets');
    expect(req.request.method).toBe('GET');
    req.flush(mockWallets);
  });

  it('should GET wallet by id', () => {
    const mockWallet = { id: '1', name: 'Test', type: 'SAVINGS' as const, balance: '100.00', user_id: '1', created_at: '', updated_at: '' };

    service.findById('1').subscribe(wallet => {
      expect(wallet).toEqual(mockWallet);
    });

    const req = httpMock.expectOne('/api/v1/wallets/1');
    expect(req.request.method).toBe('GET');
    req.flush(mockWallet);
  });

  it('should POST to create wallet', () => {
    const mockWallet = { id: '1', name: 'New', type: 'CASH' as const, balance: '0.00', user_id: '1', created_at: '', updated_at: '' };
    const createRequest = { name: 'New', type: 'CASH' as const };

    service.create(createRequest).subscribe(wallet => {
      expect(wallet).toEqual(mockWallet);
    });

    const req = httpMock.expectOne('/api/v1/wallets');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(createRequest);
    req.flush(mockWallet);
  });

  it('should PATCH to update wallet', () => {
    const mockWallet = { id: '1', name: 'Updated', type: 'SAVINGS' as const, balance: '100.00', user_id: '1', created_at: '', updated_at: '' };
    const updateRequest = { name: 'Updated' };

    service.update('1', updateRequest).subscribe(wallet => {
      expect(wallet).toEqual(mockWallet);
    });

    const req = httpMock.expectOne('/api/v1/wallets/1');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual(updateRequest);
    req.flush(mockWallet);
  });

  it('should DELETE wallet', () => {
    service.delete('1').subscribe();

    const req = httpMock.expectOne('/api/v1/wallets/1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should handle error on findAll', () => {
    service.findAll().subscribe({
      error: (err) => {
        expect(err.status).toBe(500);
      }
    });

    const req = httpMock.expectOne('/api/v1/wallets');
    req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
  });
});
