
const API_BASE_URL = 'http://localhost:8080/api/auctions';

export async function getAllAuctions() {
  const response = await fetch(API_BASE_URL);

  if (!response.ok) {
    throw new Error(`Failed to fetch auctions: ${response.status}`);
  }

  return response.json();
}


export async function getAuctionById(auctionId) {
  const response = await fetch(`${API_BASE_URL}/${auctionId}`);

  if (!response.ok) {
    throw new Error(`Failed to fetch auction: ${response.status}`);
  }

  return response.json();
}

export async function placeBid(auctionId, bidderId, amount) {
  const response = await fetch('http://localhost:8080/api/bids', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      auctionId: Number(auctionId),
      bidderId: Number(bidderId),
      amount: Number(amount),
    }),
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Bid failed: ${response.status}`);
  }

  const contentType = response.headers.get('content-type');

  if (contentType?.includes('application/json')) {
    return response.json();
  }

  return null;
}
