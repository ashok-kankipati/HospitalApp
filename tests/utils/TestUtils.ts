import { Page } from '@playwright/test';

export class TestUtils {
  static readonly BASE_URL = 'http://localhost:8080';

  /**
   * Generate a unique identifier for test data
   */
  static generateUniqueId(): string {
    return `test_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * Generate a unique email
   */
  static generateUniqueEmail(): string {
    const timestamp = Date.now();
    const random = Math.random().toString(36).substr(2, 9);
    return `test${timestamp}${random}@hospital.test`;
  }

  /**
   * Generate a unique username
   */
  static generateUniqueUsername(): string {
    return `user_${this.generateUniqueId()}`;
  }

  /**
   * Wait for network idle
   */
  static async waitForNetworkIdle(page: Page, timeout: number = 5000) {
    try {
      await page.waitForLoadState('networkidle', { timeout });
    } catch (e) {
      console.log('Network idle timeout - continuing anyway');
    }
  }

  /**
   * Check if element exists
   */
  static async elementExists(page: Page, selector: string): Promise<boolean> {
    return await page.locator(selector).count().then(count => count > 0);
  }

  /**
   * Get page load time
   */
  static async getPageLoadTime(page: Page): Promise<number> {
    const navigationTiming = await page.evaluate(() => {
      const navigation = window.performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
      return navigation.loadEventEnd - navigation.fetchStart;
    }).catch(() => 0);
    
    return navigationTiming;
  }

  /**
   * Get all console messages
   */
  static async getConsoleMessages(page: Page): Promise<Array<{type: string, message: string}>> {
    const messages: Array<{type: string, message: string}> = [];
    
    page.on('console', msg => {
      messages.push({
        type: msg.type(),
        message: msg.text()
      });
    });

    return messages;
  }

  /**
   * Capture performance metrics
   */
  static async getPerformanceMetrics(page: Page) {
    return await page.evaluate(() => {
      const navigation = window.performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
      const paint = window.performance.getEntriesByType('paint');

      return {
        dnsLookup: navigation.domainLookupEnd - navigation.domainLookupStart,
        tcpConnection: navigation.connectEnd - navigation.connectStart,
        timeToFirstByte: navigation.responseStart - navigation.requestStart,
        responseTime: navigation.responseEnd - navigation.responseStart,
        domInteractive: navigation.domInteractive - navigation.fetchStart,
        domContentLoaded: navigation.domContentLoadedEventEnd - navigation.fetchStart,
        pageLoadTime: navigation.loadEventEnd - navigation.fetchStart,
        firstPaint: paint.find(p => p.name === 'first-paint')?.startTime || 0,
        firstContentfulPaint: paint.find(p => p.name === 'first-contentful-paint')?.startTime || 0,
      };
    }).catch(() => ({}));
  }

  /**
   * Format test data
   */
  static formatTestData(data: any): string {
    return JSON.stringify(data, null, 2);
  }
}
