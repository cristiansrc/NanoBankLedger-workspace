import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CategoryService } from './category.service';
import { Category } from '../models/category.models';

describe('CategoryService', () => {
  let service: CategoryService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CategoryService]
    });
    service = TestBed.inject(CategoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => expect(service).toBeTruthy());

  it('should GET all categories', () => {
    const mockCategories: Category[] = [
      { id: '1', name: 'Salario', type: 'INCOME' as const, icon: 'briefcase', color: '#4CAF50' },
      { id: '2', name: 'Comida', type: 'EXPENSE' as const, icon: 'food', color: '#FF5722' }
    ];

    service.findAll().subscribe(categories => {
      expect(categories.length).toBe(2);
      expect(categories).toEqual(mockCategories);
    });

    const req = httpMock.expectOne('/api/v1/categories');
    expect(req.request.method).toBe('GET');
    req.flush(mockCategories);
  });

  it('should GET category by id', () => {
    const mockCategory: Category = { id: '1', name: 'Salario', type: 'INCOME' as const, icon: 'briefcase', color: '#4CAF50' };

    service.findById('1').subscribe(category => {
      expect(category).toEqual(mockCategory);
    });

    const req = httpMock.expectOne('/api/v1/categories/1');
    expect(req.request.method).toBe('GET');
    req.flush(mockCategory);
  });

  it('should return empty array when no categories exist', () => {
    service.findAll().subscribe(categories => {
      expect(categories).toEqual([]);
    });

    const req = httpMock.expectOne('/api/v1/categories');
    req.flush([]);
  });

  it('should handle error on findAll', () => {
    service.findAll().subscribe({
      error: (err) => {
        expect(err.status).toBe(500);
      }
    });

    const req = httpMock.expectOne('/api/v1/categories');
    req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
  });

  it('should handle error on findById', () => {
    service.findById('999').subscribe({
      error: (err) => {
        expect(err.status).toBe(404);
      }
    });

    const req = httpMock.expectOne('/api/v1/categories/999');
    req.flush('Not found', { status: 404, statusText: 'Not Found' });
  });
});
